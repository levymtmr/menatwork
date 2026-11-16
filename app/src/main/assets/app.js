'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const native = typeof Android !== 'undefined';
  let state, currentPage = 'today', settingsDirty = false, historySignature = '', editorOpen = false;
  const defaults = {currency:'AUD',hourly:32,tax:15,consumption:8,fuelPrice:1.85,tyres:.025,maintenance:.05,depreciation:.10};
  const emptyTotals = {gross:0,tax:0,fuel:0,tyres:0,maintenance:0,depreciation:0,extras:0,carCosts:0,net:0,workedMs:0};
  const preview = {session:{},config:defaults,totals:emptyTotals,history:[],serviceRunning:false,now:Date.now(),invoiceDraft:{profile:window.invoiceDefaults.profile,number:window.invoiceDefaults.nextNumber,rate:window.invoiceDefaults.profile.rate,rows:[]},invoices:[],nextInvoiceNumber:window.invoiceDefaults.nextNumber,invoiceAddresses:window.invoiceDefaults.addresses};
  function read() {try{return native ? JSON.parse(Android.state()) : preview;}catch(e){return state||preview;}}
  function toast(text) { $('toast').textContent=text;$('toast').classList.add('show');setTimeout(()=>$('toast').classList.remove('show'),3200); }
  function command(action, payload={}) {
    if(!native){toast('Prévia visual. Instale o APK para registrar sua jornada.');return;}
    Android.command(action,JSON.stringify(payload));
    setTimeout(refresh,300);
  }
  function number(value) {
    const text=String(value).trim().replace(',', '.');
    if(!/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(text)) return NaN;
    return Number(text);
  }
  function money(n) {return (n<0?'− ':'')+'$ '+Math.abs(n).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2});}
  function time(ms) {const sec=Math.floor(Math.max(0,ms)/1000);return [Math.floor(sec/3600),Math.floor(sec/60)%60,sec%60].map(n=>String(n).padStart(2,'0')).join(':');}
  function safeText(tag,text,cls) {const el=document.createElement(tag);el.textContent=text;if(cls)el.className=cls;return el;}
  function switchPage(page) {
    currentPage=page;
    document.querySelectorAll('.page').forEach(el=>el.classList.toggle('hidden',el.id!==page));
    document.querySelectorAll('nav button').forEach(el=>{el.classList.toggle('active',el.dataset.page===page);el.setAttribute('aria-current',el.dataset.page===page?'page':'false');});
    document.querySelector('main').scrollTo({top:0,behavior:'instant'});
    refresh();
  }
  window.goHome=()=>{if(window.JournalUI?.back())return true;if(currentPage==='today')return false;switchPage('today');return true;};
  document.querySelectorAll('nav button').forEach(el=>el.addEventListener('click',()=>switchPage(el.dataset.page)));
  function button(text,action,cls='primary') {const b=safeText('button',text,'button '+cls);b.type='button';b.onclick=()=>command(action);return b;}
  let actionsPhase='';
  function renderActions(phase) {
    if(actionsPhase===phase)return;
    actionsPhase=phase;
    const box=$('actions');box.replaceChildren();
    if(phase==='idle'){
      box.append(button('↗ INICIAR TRAJETO AO TRABALHO','commute'));
      box.append(button('JÁ ESTOU NO TRABALHO · INICIAR','work','secondary'));
    }else if(phase==='commute'){
      box.append(button('▸ CHEGUEI · INICIAR TRABALHO','work'));
      box.append(button('ENCERRAR E SALVAR SÓ O TRAJETO','finish','subtle'));
    }else if(phase==='work'||phase==='pause'){
      const pair=safeText('div','','pair');
      pair.append(button(phase==='work'?'Ⅱ PAUSAR':'▸ RETOMAR',phase==='work'?'pause':'resume'));
      pair.append(button('ENCERRAR','finish','secondary'));box.append(pair);
      box.append(button('⌁ TERMINAR TRABALHO E REGISTRAR VOLTA','return','subtle'));
    }else if(phase==='return'){
      box.append(button('■ CHEGUEI · SALVAR JORNADA','finish'));
    }
    if(phase!=='idle'){const fix=button('CORRIGIR HORÁRIOS / ENCERRAR','noop','subtle');fix.onclick=()=>window.JournalUI.open(state.session,true);box.append(fix,button('DESCARTAR JORNADA ATUAL','discardActive','subtle'));}
  }
  function renderRows(t,cfg) {
    const rows=[['↗','Ganho bruto',t.gross,true],['%','Imposto estimado · '+cfg.tax+'%',t.tax],['▤','Gasolina',t.fuel],['◉','Pneus',t.tyres],['⚒','Manutenção',t.maintenance],['↘','Depreciação',t.depreciation],['＋','Pedágio e outros',t.extras]];
    $('rows').replaceChildren(...rows.map(([icon,name,value,positive])=>{
      const row=safeText('div','','row'+(positive?' positive':''));
      row.append(safeText('span',icon,'icon'),safeText('span',name,'name'),safeText('strong',(positive?'':'− ')+money(value)));
      return row;
    }));
    $('deductions').textContent='− '+money(t.tax+t.carCosts);
  }
  function fillSettings() {
    const form=$('settingsForm');
    for(const [key,value]of Object.entries(state.config)){if(form.elements.namedItem(key))form.elements.namedItem(key).value=String(value).replace('.',',');}
  }
  function refresh() {
    state=read();
    const s=state.session,t=state.totals,cfg=s.config||state.config,phase=s.phase||'idle';
    document.documentElement.dataset.phase=phase;
    const names={idle:'PRONTO',commute:'EM TRAJETO',work:'TRABALHANDO',pause:'EM PAUSA',return:'VOLTANDO'};
    $('status').textContent=native?names[phase]:'PRÉVIA';$('status').classList.toggle('live',phase==='work'||phase==='commute'||phase==='return');
    $('todayDate').textContent=new Date(state.now).toLocaleDateString('pt-BR',{day:'2-digit',month:'2-digit'});
    $('net').textContent=money(t.net);$('net').classList.toggle('negative',t.net<0);
    $('currencyBadge').textContent=cfg.currency;$('timer').textContent=time(t.workedMs);$('hourly').textContent=money(cfg.hourly);
    const notes={idle:'Comece pelo trajeto ou inicie direto no trabalho.',commute:'Registrando ida. Toque em “Cheguei” quando começar a trabalhar.',work:'Jornada ativa. Seu ganho acompanha o tempo, mesmo com a tela apagada.',pause:'Pausa ativa. O tempo pago está parado.',return:'Trabalho finalizado. Registrando o custo da volta.'};
    $('phaseNote').textContent=notes[phase];
    const daily={};
    const dateKey=ts=>{const d=new Date(ts);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
    for(const record of state.history){if(!record.deleted&&(record.workDate||dateKey(record.startedAt))===dateKey(state.now))daily[record.config.currency]=(daily[record.config.currency]||0)+record.totals.net;}
    if(phase!=='idle'&&(s.workDate||dateKey(s.startedAt))===dateKey(state.now))daily[cfg.currency]=(daily[cfg.currency]||0)+t.net;
    const summary=$('dailySummary');summary.classList.toggle('hidden',!Object.keys(daily).length);
    summary.replaceChildren(safeText('span','ACUMULADO HOJE','tiny'),...Object.entries(daily).map(([currency,value])=>safeText('strong',currency+' '+money(value))),safeText('small','Por dia de trabalho · inclui jornada atual'));
    $('recovery').classList.toggle('hidden',phase==='idle'||state.serviceRunning);
    renderRows(t,cfg);renderActions(phase);
    $('distance').replaceChildren(document.createTextNode((s.km||0).toLocaleString('pt-BR',{minimumFractionDigits:1,maximumFractionDigits:1})+' '),safeText('small','km'));
    const travelling=phase==='commute'||phase==='return';
    const fresh=s.lastFix&&state.now-s.lastFix<30000;
    $('gpsBadge').textContent=travelling?(fresh?'GPS ATIVO':'BUSCANDO GPS'):'GPS EM ESPERA';
    $('gpsBadge').classList.toggle('active',travelling&&fresh);
    $('gpsNote').textContent=travelling?(fresh?s.gpsNote:(s.gpsNote&&s.gpsNote.includes('confira')?s.gpsNote:'Aguardando sinal. Use o GPS ao ar livre e confira os km.')):phase==='idle'?'Registre a distância real com GPS ou informe os km.':(s.hasGpsGap?'Houve perda de sinal. Confira e ajuste a distância total.':'Distância acumulada de ida e volta. Ajuste os km se necessário.');
    $('routeOrigin').textContent=phase==='return'?'Trabalho':'Partida variável';
    $('routeDestination').textContent=phase==='return'?'Destino de volta':'Trabalho';
    $('editCosts').disabled=phase==='idle';
    if(!settingsDirty)fillSettings();
    window.JournalUI.render(state);window.SkillsUI.render(state);
    if(window.InvoiceUI)window.InvoiceUI.render(state);
    if(window.AppUpdateUI)window.AppUpdateUI.render(state.update);
  }
  $('editCosts').onclick=()=>{editorOpen=!editorOpen;$('costEditor').classList.toggle('hidden',!editorOpen);if(editorOpen){$('manualWorksite').value=state.session.worksite||'';$('manualKm').value=String(Math.round((state.session.km||0)*1000)/1000).replace('.',',');$('manualExtras').value=String(state.session.extras||0).replace('.',',');$('manualHours').value=String(Math.round(state.totals.workedMs/3600000*10000)/10000).replace('.',',');}};
  $('saveCosts').onclick=()=>{
    const km=number($('manualKm').value),extras=number($('manualExtras').value),hours=number($('manualHours').value);
    if(!Number.isFinite(km)||km>100000||!Number.isFinite(extras)||extras>1000000||!Number.isFinite(hours)||hours>100000){toast('Informe distância, gastos e horas válidos, usando vírgula ou ponto.');return;}
    command('updateCosts',{km,extras,workedMs:Math.round(hours*3600000),worksite:$('manualWorksite').value.trim()});editorOpen=false;$('costEditor').classList.add('hidden');
  };
  $('settingsForm').oninput=()=>{settingsDirty=true;};
  $('settingsForm').onsubmit=e=>{
    e.preventDefault();const form=e.currentTarget,p={currency:form.elements.currency.value};
    const limits={hourly:10000,tax:100,consumption:100,fuelPrice:1000,tyres:1000,maintenance:1000,depreciation:1000};
    for(const [key,max]of Object.entries(limits)){
      p[key]=number(form.elements.namedItem(key).value);
      if(!Number.isFinite(p[key])||p[key]>max||((key==='hourly'||key==='consumption')&&p[key]<=0)){$('settingsError').textContent='Confira os valores. Hora e consumo devem ser maiores que zero; imposto entre 0 e 100%.';return;}
    }
    $('settingsError').textContent='';command('saveConfig',p);settingsDirty=false;
  };
  $('recover').onclick=()=>command('recover');$('export').onclick=()=>command('export');$('appSettings').onclick=()=>command('appSettings');
  window.App={state:()=>state,go:switchPage,refresh,toast,command};
  refresh();setInterval(()=>{if(!document.hidden)refresh();},1000);document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
})();
