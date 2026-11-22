'use strict';
(() => {
  const $=id=>document.getElementById(id), native=typeof Android!=='undefined';
  let state,initialized=false,saveTimer,recordsSignature='',editing=false,limit=20,trashLimit=20,fieldCounter=0,pickerLimit=30,pickerSignature='',pendingEdit=false,pendingNew=null,pendingAddId=null,saveRevision=0;
  const filters=window.Listing.controls('invoices',()=>{limit=20;trashLimit=20;recordsSignature='';if(state)records();});
  const fields={
    name:'Nome do prestador',address:'Endereço do prestador',email:'E-mail do prestador',abn:'ABN do prestador',
    bank:'Banco',account_name:'Nome da conta',bsb:'BSB',account:'Número da conta',
    client:'Nome do cliente',client_address:'Endereço do cliente',client_abn:'ABN do cliente',client_email:'E-mail do cliente',description:'Descrição dos serviços (em inglês)',timesheet_position:'Cargo no timesheet (em inglês)',timesheet_company:'Empresa no timesheet',timesheet_location:'Local no timesheet'
  };
  function text(tag,value,cls){const e=document.createElement(tag);e.textContent=value;if(cls)e.className=cls;return e;}
  function userText(e){e.dataset.userText='';return e;}
  function error(message){$('invoiceError').textContent=message||'';}
  window.invoiceError=error;
  function cmd(action,payload){if(!native){error('Prévia visual. Instale o APK para gerar, guardar e enviar PDFs.');return;}Android.command(action,JSON.stringify(payload||{}));}
  const today=()=>state&&state.invoiceToday||new Intl.DateTimeFormat('en-CA',{timeZone:'Australia/Adelaide',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  function decimal(value){const s=String(value).trim().replace(',','.');if(!/^\d+(?:\.\d+)?$/.test(s)||s.length>32)throw Error('Use um número positivo, com vírgula ou ponto.');const [a,b='']=s.split('.');return{n:BigInt(a+b),d:10n**BigInt(b.length)};}
  function hours(value){const s=String(value).trim();if(/^\d{1,2}:[0-5]\d$/.test(s)){const [h,m]=s.split(':').map(Number);if(h*60+m>1440)throw Error('Cada lançamento aceita até 24 horas.');return{n:BigInt(h*60+m),d:60n};}const r=decimal(s||'0');if(r.n>24n*r.d)throw Error('Cada lançamento aceita até 24 horas.');return r;}
  function line(row,rate){
    const dayOff=row.address.trim().toLowerCase()==='day off',h=dayOff?{n:0n,d:1n}:hours(row.hours);
    const r=decimal(row.sourceRate||rate);if(r.n<=0n||r.n>10000n*r.d)throw Error('Confira a tarifa do invoice.');
    const quarters=(h.n*4n+h.d-1n)/h.d;
    const denominator=row.exactHours?h.d*r.d:4n*r.d,numerator=row.exactHours?h.n*r.n*100n:quarters*r.n*100n;
    const premium=decimal(row.overtimePremium||0),combined=numerator*premium.d+premium.n*100n*denominator,divisor=denominator*premium.d;const cents=(combined*2n+divisor)/(divisor*2n);
    return {actual:Number(h.n)/Number(h.d),quarters:row.exactHours?Number(h.n)*4/Number(h.d):Number(quarters),cents:Number(cents),dayOff};
  }
  const cash=cents=>'$ '+(cents/100).toLocaleString(window.I18n.locale(),{minimumFractionDigits:2,maximumFractionDigits:2});
  function input(label,name,value,parent,kind='text'){
    const wrap=text('label',label),el=document.createElement(kind==='textarea'?'textarea':'input');
    if(kind!=='textarea')el.type=kind;el.id='invoice-field-'+(++fieldCounter)+'-'+name;wrap.htmlFor=el.id;el.autocomplete='off';el.spellcheck=false;el.name=name;el.value=value||'';el.maxLength=name.includes('address')?350:name==='description'?500:name.includes('email')?160:name.includes('abn')||name==='bsb'?40:name==='account'?80:120;
    wrap.append(el);parent.append(wrap);return el;
  }
  for(const [key,label]of Object.entries(fields)){const target=key==='client'||key==='client_email'?'clientIdentity':key==='description'?'serviceFields':key.startsWith('timesheet_')?'timesheetFields':key.startsWith('client')?'clientFields':'sellerFields';input(label,key,'',$(target),key.includes('address')||key==='description'?'textarea':key.includes('email')?'email':'text');}
  function readDraft(){
    const profile={};for(const key of Object.keys(fields))profile[key]=document.querySelector(`[name="${key}"]`).value.trim();
    const rows=[...document.querySelectorAll('.invoice-entry')].map(el=>({date:el.querySelector('[name="date"]').value,address:el.querySelector('[name="workAddress"]').value.trim(),hours:el.querySelector('[name="hours"]').value.trim()||'0',sourceId:el.dataset.sourceId||'',sourceFingerprint:el.dataset.sourceFingerprint||'',workType:el.dataset.workType||'',overtimePremium:el.dataset.overtimePremium||'0',exactHours:el.dataset.exactHours==='true',billingNote:el.dataset.billingNote||'',...(el.dataset.timesheet?{timesheet:JSON.parse(el.dataset.timesheet)}:{}),...(el.dataset.sourceRate?{sourceRate:el.dataset.sourceRate}:{})}));
    return {number:Number($('invoiceNumber').value),rate:$('invoiceRate').value.trim().replace(',','.'),profile,rows};
  }
  function markenCents(rows){const groups=new Map(),gcd=(a,b)=>b?gcd(b,a%b):a;for(const row of rows){if(!row.timesheet)throw Error('Para Marken, vincule jornadas com início e término para gerar os dois PDFs.');const e=row.timesheet,r=decimal(e.rate),div=gcd(r.n,r.d),key=(r.n/div)+'/'+(r.d/div);let g=groups.get(key);if(!g){g={r,ms:[0n,0n,0n]};groups.set(key,g);}for(const [i,k]of ['regularMs','ot15Ms','ot2Ms'].entries())g.ms[i]+=BigInt(e[k]);}let total=0n;for(const g of groups.values())for(let i=0;i<3;i++){const n=g.ms[i]*g.r.n*BigInt([2,3,4][i])*100n,d=3600000n*g.r.d*2n;total+=(n*2n+d)/(d*2n);}return Number(total);}
  function updateTotals(){
    const draft=readDraft();let actual=0,quarters=0,cents=0,invalid=false;
    draft.rows.forEach((r,i)=>{const target=$('invoiceRows').children[i].querySelector('.line-total');try{const l=line(r,draft.rate);actual+=l.actual;quarters+=l.quarters;cents+=l.cents;target.textContent=l.dayOff?'Day off · '+cash(0):(r.exactHours?l.actual.toLocaleString(window.I18n.locale(),{maximumFractionDigits:6}):l.quarters/4)+' h cobradas · '+cash(l.cents);}catch(e){target.textContent=e.message;invalid=true;}});
    const pair=draft.rows.some(r=>r.timesheet)||/\bmarken\b/i.test(draft.profile.client);$('markenTotalHint').classList.toggle('hidden',!pair);$('timesheetPreview').classList.toggle('hidden',!pair);$('timesheetMetaPanel').classList.toggle('hidden',!pair);$('invoiceManualHint').classList.toggle('hidden',!pair);$('invoiceAddRow').disabled=pair;$('invoiceRowCount').textContent=draft.rows.length+' itens';$('invoiceEmpty').classList.toggle('hidden',draft.rows.length>0);renderPicker();$('invoiceIssue').textContent=pair?'EMITIR INVOICE + TIMESHEET':'EMITIR E GUARDAR PDF';if(pair)try{cents=markenCents(draft.rows);}catch(e){invalid=true;error(e.message);}
    $('invoiceActual').textContent=actual.toLocaleString(window.I18n.locale(),{maximumFractionDigits:4})+' h';$('invoiceBilled').textContent=(quarters/4).toLocaleString(window.I18n.locale(),{maximumFractionDigits:2})+' h';$('invoiceTotal').textContent=invalid?'Confira as linhas':cash(cents);
  }
  function save(){clearTimeout(saveTimer);if(native&&initialized)cmd('saveInvoiceDraft',{...readDraft(),_saveRevision:saveRevision});}
  function changed(){editing=true;saveRevision++;$('invoiceSaveState').textContent='SALVANDO...';updateTotals();clearTimeout(saveTimer);saveTimer=setTimeout(save,500);}
  function addRow(row={date:today(),address:'',hours:'0',sourceId:''},persist=true){
    const box=text('div','','invoice-entry');box.dataset.sourceId=row.sourceId||'';box.dataset.sourceFingerprint=row.sourceFingerprint||'';box.dataset.workType=row.workType||'';box.dataset.overtimePremium=Number(row.overtimePremium||0).toFixed(10);box.dataset.sourceRate=row.sourceRate||'';box.dataset.exactHours=!!row.exactHours;box.dataset.billingNote=row.billingNote||'';box.dataset.timesheet=row.timesheet?JSON.stringify(row.timesheet):'';
    const top=text('div','','entry-top');top.append(text('span',row.sourceId?'JORNADA VINCULADA':'ITEM MANUAL'));if(row.sourceId){const edit=text('button','EDITAR JORNADA','button secondary edit-linked');edit.type='button';edit.onclick=()=>editJourney(row.sourceId);top.append(edit);}
    const remove=text('button','×','remove-entry');remove.type='button';remove.setAttribute('aria-label','Remover lançamento');remove.onclick=()=>{box.remove();changed();};top.append(remove);box.append(top);
    const grid=text('div','','invoice-grid');const dateInput=input('Data','date',row.date,grid,'date');dateInput.readOnly=!!row.timesheet;const h=input(row.sourceRate?'Horas da jornada':'Horas pagas','hours',row.hours,grid);h.readOnly=!!row.sourceRate;h.inputMode='text';h.placeholder='7,5 ou 07:30';if(h.readOnly){const hint=text('p','Use Editar jornada para ajustar os horários e recalcular o valor.','help');hint.id=h.id+'-hint';h.setAttribute('aria-describedby',hint.id);h.onclick=()=>editJourney(row.sourceId);box.append(hint);}h.maxLength=32;box.append(grid);
    const addr=input('Local do trabalho','workAddress',row.address,box);addr.maxLength=1000;window.WorksitesUI.bind(addr);
    box.append(text('p','','line-total'));$('invoiceRows').append(box);if(persist)changed();return box;
  }
  function populate(draft){
    $('invoiceNumber').value=draft.number||state.nextInvoiceNumber||18;$('invoiceRate').value=(window.I18n.language()==='pt'?String(draft.rate||'32').replace('.',','):String(draft.rate||'32'));
    for(const key of Object.keys(fields))document.querySelector(`[name="${key}"]`).value=draft.profile&&draft.profile[key]||'';
    $('invoiceRows').replaceChildren();for(const r of draft.rows||[])addRow(r,false);
    initialized=true;editing=false;$('invoiceSaveState').textContent='RASCUNHO LOCAL';updateTotals();
  }
  function validDraft(){
    const d=readDraft();if(!/^[1-9]\d{0,8}$/.test($('invoiceNumber').value.trim()))throw Error('Confira o número do invoice.');
    const sources=new Set();let worked=false;
    for(const r of d.rows){const l=line(r,d.rate);if(l.actual>0){worked=true;if(!r.address)throw Error('Preencha o local de cada trabalho.');}if((l.actual>0||l.dayOff)&&!/^\d{4}-\d{2}-\d{2}$/.test(r.date))throw Error('Confira as datas dos lançamentos.');if(r.sourceId){if(sources.has(r.sourceId))throw Error('Uma jornada está repetida.');sources.add(r.sourceId);}}
    if(!worked)throw Error('Inclua pelo menos um trabalho com horas.');
    for(const key of ['name','client','description','bank','account_name','bsb','account'])if(!d.profile[key])throw Error('Confira os dados de prestador, cliente e pagamento.');
    return d;
  }
  function perform(action){error('');try{const draft=validDraft();clearTimeout(saveTimer);cmd(action,draft);}catch(e){error(e.message);}}
  function recordButton(label,action,filename,cls='secondary',extra={}){const b=text('button',label,'button '+cls);b.onclick=()=>{clearTimeout(saveTimer);cmd(action,{filename,...extra});};return b;}
  function records(){
    const list=state.invoices||[],signature=JSON.stringify([list,state.history,filters,limit,trashLimit,window.I18n.language()]);if(signature===recordsSignature)return;recordsSignature=signature;
    const active=window.Listing.invoices(list.filter(r=>r.status!=='cancelled'),filters,state),removed=window.Listing.invoices(list.filter(r=>r.status==='cancelled'),filters,state);$('invoiceCount').textContent=Math.min(limit,active.length)+' / '+active.length+' invoices';$('invoiceMore').classList.toggle('hidden',active.length<=limit);$('invoiceTrashMore').classList.toggle('hidden',removed.length<=trashLimit);
    const total=items=>items.reduce((sum,r)=>sum+Math.round(Number(r.total)*100),0);$('invoiceLedgerTotals').textContent=active.length+' invoices · AUD '+cash(total(active))+'\nPagos: '+cash(total(active.filter(r=>r.paid)))+' · A receber: '+cash(total(active.filter(r=>!r.paid)));
    $('invoiceTrash').classList.toggle('hidden',!removed.length);$('invoiceTrashCount').textContent=removed.length;
    for(const [target,items] of [[$('invoiceRecords'),active.slice(0,limit)],[$('invoiceTrashList'),removed.slice(0,trashLimit)]]){
      target.replaceChildren();if(!items.length)target.append(text('p','Nenhum invoice neste grupo.','empty'));
      for(const r of items){
        const cancelled=r.status==='cancelled',card=text('article','','panel history-card');card.append(text('h3','INVOICE #'+String(r.number).padStart(3,'0')),userText(text('p',r.client,'muted record-client')),text('p',cash(Math.round(Number(r.total)*100)),'history-gross'),text('p','Emissão: '+r.issued+' · AUD','muted record-date'));
        if(r.timesheetFilename)card.append(text('p',r.signedTimesheetFilename?'Timesheet assinado anexado':'Timesheet gerado · falta assinatura','help'));
        if(!cancelled)card.append(text('p',r.paid?'✓ PAGO':'◷ AGUARDANDO PAGAMENTO','billing-badge '+(r.paid?'paid':'unpaid')));
        if(cancelled)card.append(text('p','CANCELADO · não entra no total. O PDF original e o número ficam preservados.','help'));
        if(r.needsReview)card.append(text('p','REVISÃO NECESSÁRIA: uma jornada vinculada mudou. Crie uma correção antes de enviar.','notice'));
        const actions=text('div','','invoice-record-actions');actions.append(recordButton('ABRIR PDF','viewInvoice',r.filename),recordButton('SALVAR CÓPIA','saveInvoicePdf',r.filename));
        if(r.timesheetFilename){actions.append(recordButton('ABRIR TIMESHEET','viewTimesheet',r.filename),recordButton('SALVAR TIMESHEET','saveTimesheetPdf',r.filename));if(!cancelled&&!r.needsReview)actions.append(recordButton('ANEXAR TIMESHEET ASSINADO','attachSignedTimesheet',r.filename));}
        if(cancelled)actions.append(recordButton('RESTAURAR','restoreInvoice',r.filename),recordButton('CRIAR CORREÇÃO','correctInvoice',r.filename));else{
          if(!r.needsReview)actions.append(recordButton('↗ ENVIAR AO CLIENTE','shareInvoice',r.filename,'primary send'));
          actions.append(recordButton(r.paid?'MARCAR COMO NÃO PAGO':'✓ MARCAR COMO PAGO','setInvoicePaid',r.filename,'secondary payment-action',{paid:!r.paid}));
          actions.append(recordButton('CORRIGIR / NOVO RASCUNHO','correctInvoice',r.filename),recordButton('REMOVER','cancelInvoice',r.filename,'subtle'));
        }
        card.append(actions);window.FoldUI.record(card,'invoice-'+r.filename);target.append(card);
      }
    }
  }
  function sourceRow(s){return {date:s.workDate||today(),...(s.timesheetEntry?{timesheet:s.timesheetEntry}:{}),address:s.worksite||'',hours:(s.workedMs/3600000).toFixed(10).replace(/0+$/,'').replace(/\.$/,''),sourceId:String(s.id||s.startedAt),sourceFingerprint:s.billingFingerprint||'',workType:s.workType||'',overtimePremium:s.totals?.overtimePremium||0,exactHours:s.config?.payRule==='marken',billingNote:s.billingNote||'',...(s.config?.overtime?{sourceRate:s.config.hourly}:{})};}
  function editJourney(id){const source=(state.history||[]).find(r=>String(r.id||r.startedAt)===id&&!r.deleted);if(!source){error('A jornada não está disponível. Atualize a lista.');return;}pendingEdit=true;window.JournalUI.open(source);}
  function renderPicker(){if(!state)return;const selectedClient=window.Listing.normal(document.querySelector('#invoices [name="client"]').value),query=window.Listing.normal($('invoiceJourneySearch').value),included=new Set([...$('invoiceRows').children].map(e=>e.dataset.sourceId)),claimed=new Set((state.invoices||[]).filter(r=>r.status!=='cancelled').flatMap(r=>r.sourceIds||[]).map(String));const eligible=(state.history||[]).filter(r=>!r.deleted&&r.workedMs>0&&r.config?.currency==='AUD'&&!claimed.has(String(r.id||r.startedAt))&&window.Listing.normal([r.client,r.workDate,r.workType,r.worksite].join(' ')).includes(query)).sort((a,b)=>Number(window.Listing.normal(b.client||'')===selectedClient)-Number(window.Listing.normal(a.client||'')===selectedClient)||(b.workDate||'').localeCompare(a.workDate||''));const signature=JSON.stringify([eligible.map(r=>[r.id,r.billingFingerprint,r.workedMs]),[...included],query,selectedClient,pickerLimit,window.I18n.language()]);if(signature===pickerSignature)return;pickerSignature=signature;const list=$('invoiceJourneyList');list.replaceChildren();$('invoiceJourneyMore').classList.toggle('hidden',eligible.length<=pickerLimit);if(!eligible.length)list.append(text('p','Nenhuma jornada disponível. Crie uma jornada ou ajuste a busca.','help'));for(const r of eligible.slice(0,pickerLimit)){const row=text('div','','journey-choice'),info=text('div','','');info.dataset.userText='';info.append(text('strong',r.workDate||''),text('small',[r.client,r.worksite].filter(Boolean).join(' · ')));const incomplete=r.config?.payRule==='marken'&&!r.timesheetEntry;const button=text('button',included.has(String(r.id||r.startedAt))?'ADICIONADA':incomplete?'COMPLETAR HORÁRIOS':'ADICIONAR','button secondary');button.type='button';button.disabled=included.has(String(r.id||r.startedAt));button.onclick=()=>{if(incomplete){pendingAddId=String(r.id||r.startedAt);editJourney(pendingAddId);}else window.InvoiceUI.addJourney(r);};row.append(info,button);list.append(row);}}
  function journeySaved(){if(!pendingEdit&&!pendingNew)return;state=window.App.state();for(const old of [...$('invoiceRows').children]){if(!old.dataset.sourceId)continue;const source=(state.history||[]).find(r=>String(r.id||r.startedAt)===old.dataset.sourceId&&!r.deleted);if(source){const next=sourceRow(source);next.address=old.querySelector('[name="workAddress"]').value||next.address;const replacement=addRow(next,false);old.replaceWith(replacement);}}if(pendingNew){const created=(state.history||[]).find(r=>!pendingNew.has(String(r.id||r.startedAt))&&!r.deleted);if(created)window.InvoiceUI.addJourney(created);}if(pendingAddId){const completed=(state.history||[]).find(r=>String(r.id||r.startedAt)===pendingAddId);if(completed)window.InvoiceUI.addJourney(completed);}pendingEdit=false;pendingNew=null;pendingAddId=null;changed();}
  $('invoiceJourneySearch').oninput=()=>{pickerLimit=30;pickerSignature='';renderPicker();};$('invoiceJourneyMore').onclick=()=>{pickerLimit+=30;pickerSignature='';renderPicker();};$('invoicePickJourneys').onclick=()=>{$('invoiceJourneyPicker').open=!$('invoiceJourneyPicker').open;if($('invoiceJourneyPicker').open)$('invoiceJourneySearch').focus();};$('invoiceCreateJourney').onclick=()=>{pendingNew=new Set((state.history||[]).map(r=>String(r.id||r.startedAt)));window.JournalUI.open();const client=document.querySelector('#invoices [name="client"]').value.trim().toLocaleLowerCase('en');const select=document.querySelector('#journalForm .client-select');if([...select.options].some(o=>o.value===client)){select.value=client;select.dispatchEvent(new Event('change',{bubbles:true}));}};
  window.invoiceDraftSaved=revision=>{if(revision===saveRevision){editing=false;$('invoiceSaveState').textContent='RASCUNHO SALVO';}};
  window.InvoiceUI={journeySaved,journeyCancelled(){pendingEdit=false;pendingNew=null;pendingAddId=null;},
    selectClient(profile){if(profile.hourly>0)$('invoiceRate').value=profile.hourly;for(const key of ['client','client_address','client_abn','client_email'])document.querySelector('#invoices [name="'+key+'"]').value=profile[key]||'';changed();},
    render(s){state=s;window.Listing.options($('invoices-client'),(s.invoices||[]).map(r=>r.client));window.Listing.options($('invoices-type'),(s.invoices||[]).flatMap(r=>window.Listing.invoiceTypes(r,s)));if(!initialized)populate(s.invoiceDraft||{profile:window.invoiceDefaults.profile,number:s.nextInvoiceNumber||window.invoiceDefaults.nextNumber,rate:window.invoiceDefaults.profile.rate,rows:[]});$('invoiceDate').textContent=today();records();renderPicker();},
    addJourney(s){
      const d=readDraft(),id=String(s.id||s.startedAt);
      if(s.config&&s.config.currency!=='AUD'){error('A jornada usa outra moeda. Preencha manualmente uma tarifa em AUD; o app não converte moedas.');return false;}
      if(s.deleted){error('Restaure a jornada antes de adicioná-la.');return false;}
      const already=(state.invoices||[]).find(r=>r.status!=='cancelled'&&(r.sourceIds||[]).includes(id));
      if(already||d.rows.some(r=>r.sourceId===id)){error('Essa jornada já foi adicionada a um invoice.');return false;}
      const date=s.workDate||new Intl.DateTimeFormat('en-CA',{timeZone:'Australia/Adelaide',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(s.startedAt));
      if(s.config?.payRule==='marken'&&!s.timesheetEntry){error(s.timesheetError||'Atualize as jornadas vinculadas para gerar o timesheet correto.');return false;}
      error('');addRow(sourceRow(s));window.FoldUI.expand(document.querySelector('#invoices > .panel.settings-panel'));return true;
    }
  };
  window.invoiceIssued=()=>{window.FoldUI.expand(document.querySelector('#invoices > .panel.settings-panel'));clearTimeout(saveTimer);initialized=false;editing=false;recordsSignature='';error('');};
  $('invoices').addEventListener('input',e=>{if(e.target.matches('input,textarea'))changed();});
  $('invoiceMore').onclick=()=>{limit+=20;recordsSignature='';records();};$('invoiceTrashMore').onclick=()=>{trashLimit+=20;recordsSignature='';records();};
  new IntersectionObserver(entries=>{if(entries.some(e=>e.isIntersecting)&&!$('invoices').classList.contains('hidden')&&!$('invoiceMore').classList.contains('hidden'))$('invoiceMore').click();},{root:document.querySelector('main'),rootMargin:'120px'}).observe($('invoiceMore'));
  $('invoiceRefreshSources').onclick=()=>{clearTimeout(saveTimer);cmd('refreshInvoiceSources',readDraft());};
  $('timesheetPreview').onclick=()=>perform('previewTimesheet');
  $('invoiceAddRow').onclick=()=>addRow();$('invoicePreview').onclick=()=>perform('previewInvoice');$('invoiceIssue').onclick=()=>perform('issueInvoice');$('invoiceClear').onclick=()=>{clearTimeout(saveTimer);cmd('newInvoiceDraft');};
  document.addEventListener('visibilitychange',()=>{if(document.hidden&&editing)save();});
})();
