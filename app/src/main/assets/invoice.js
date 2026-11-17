'use strict';
(() => {
  const $=id=>document.getElementById(id), native=typeof Android!=='undefined';
  let state,initialized=false,saveTimer,recordsSignature='',editing=false,limit=20,trashLimit=20;
  const filters=window.Listing.controls('invoices',()=>{limit=20;trashLimit=20;recordsSignature='';if(state)records();});
  const fields={
    name:'Nome do prestador',address:'Endereço do prestador',email:'E-mail do prestador',abn:'ABN do prestador',
    bank:'Banco',account_name:'Nome da conta',bsb:'BSB',account:'Número da conta',
    client:'Nome do cliente',client_address:'Endereço do cliente',client_abn:'ABN do cliente',client_email:'E-mail do cliente',description:'Descrição dos serviços (em inglês)'
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
    const r=decimal(rate);if(r.n<=0n||r.n>10000n*r.d)throw Error('Confira a tarifa do invoice.');
    const quarters=(h.n*4n+h.d-1n)/h.d;
    const denominator=4n*r.d,numerator=quarters*r.n*100n;
    const cents=(numerator*2n+denominator)/(denominator*2n);
    return {actual:Number(h.n)/Number(h.d),quarters:Number(quarters),cents:Number(cents),dayOff};
  }
  const cash=cents=>'$ '+(cents/100).toLocaleString(window.I18n.locale(),{minimumFractionDigits:2,maximumFractionDigits:2});
  function input(label,name,value,parent,kind='text'){
    const wrap=text('label',label),el=document.createElement(kind==='textarea'?'textarea':'input');
    if(kind!=='textarea')el.type=kind;el.name=name;el.value=value||'';el.maxLength=name.includes('address')?350:name==='description'?500:name.includes('email')?160:name.includes('abn')||name==='bsb'?40:name==='account'?80:120;
    wrap.append(el);parent.append(wrap);return el;
  }
  for(const [key,label]of Object.entries(fields))input(label,key,'',$((key.startsWith('client')||key==='description')?'clientFields':'sellerFields'),key.includes('address')||key==='description'?'textarea':key.includes('email')?'email':'text');
  function readDraft(){
    const profile={};for(const key of Object.keys(fields))profile[key]=document.querySelector(`[name="${key}"]`).value.trim();
    const rows=[...document.querySelectorAll('.invoice-entry')].map(el=>({date:el.querySelector('[name="date"]').value,address:el.querySelector('[name="workAddress"]').value.trim(),hours:el.querySelector('[name="hours"]').value.trim()||'0',sourceId:el.dataset.sourceId||'',sourceFingerprint:el.dataset.sourceFingerprint||'',workType:el.dataset.workType||''}));
    return {number:Number($('invoiceNumber').value),rate:$('invoiceRate').value.trim().replace(',','.'),profile,rows};
  }
  function updateTotals(){
    const draft=readDraft();let actual=0,quarters=0,cents=0,invalid=false;
    draft.rows.forEach((r,i)=>{const target=$('invoiceRows').children[i].querySelector('.line-total');try{const l=line(r,draft.rate);actual+=l.actual;quarters+=l.quarters;cents+=l.cents;target.textContent=l.dayOff?'Day off · '+cash(0):l.quarters/4+' h cobradas · '+cash(l.cents);}catch(e){target.textContent=e.message;invalid=true;}});
    $('invoiceActual').textContent=actual.toLocaleString(window.I18n.locale(),{maximumFractionDigits:4})+' h';$('invoiceBilled').textContent=(quarters/4).toLocaleString(window.I18n.locale(),{maximumFractionDigits:2})+' h';$('invoiceTotal').textContent=invalid?'Confira as linhas':cash(cents);
  }
  function save(){clearTimeout(saveTimer);if(native&&initialized)cmd('saveInvoiceDraft',readDraft());}
  function changed(){editing=true;updateTotals();clearTimeout(saveTimer);saveTimer=setTimeout(save,500);}
  function addRow(row={date:today(),address:'',hours:'0',sourceId:''},persist=true){
    const box=text('div','','invoice-entry');box.dataset.sourceId=row.sourceId||'';box.dataset.sourceFingerprint=row.sourceFingerprint||'';box.dataset.workType=row.workType||'';
    const top=text('div','','entry-top');top.append(text('span',row.sourceId?'JORNADA DO HISTÓRICO':'LANÇAMENTO MANUAL'));
    const remove=text('button','×','remove-entry');remove.type='button';remove.setAttribute('aria-label','Remover lançamento');remove.onclick=()=>{box.remove();changed();};top.append(remove);box.append(top);
    const grid=text('div','','invoice-grid');input('Data','date',row.date,grid,'date');const h=input('Horas reais','hours',row.hours,grid);h.inputMode='decimal';h.maxLength=32;box.append(grid);
    const addr=input('Local do trabalho','workAddress',row.address,box);addr.maxLength=1000;window.WorksitesUI.bind(addr);
    box.append(text('p','','line-total'));$('invoiceRows').append(box);if(persist)changed();
  }
  function populate(draft){
    $('invoiceNumber').value=draft.number||state.nextInvoiceNumber||18;$('invoiceRate').value=(window.I18n.language()==='pt'?String(draft.rate||'32').replace('.',','):String(draft.rate||'32'));
    for(const key of Object.keys(fields))document.querySelector(`[name="${key}"]`).value=draft.profile&&draft.profile[key]||'';
    $('invoiceRows').replaceChildren();for(const r of draft.rows||[])addRow(r,false);
    initialized=true;editing=false;updateTotals();
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
        const cancelled=r.status==='cancelled',card=text('article','','panel history-card');card.append(text('h3','INVOICE #'+String(r.number).padStart(3,'0')),userText(text('p',r.client,'muted record-client')),text('p',cash(Math.round(Number(r.total)*100)),'history-net'),text('p','Emissão: '+r.issued+' · AUD','muted record-date'));
        if(!cancelled)card.append(text('p',r.paid?'✓ PAGO':'◷ AGUARDANDO PAGAMENTO','billing-badge '+(r.paid?'paid':'unpaid')));
        if(cancelled)card.append(text('p','CANCELADO · não entra no total. O PDF original e o número ficam preservados.','help'));
        if(r.needsReview)card.append(text('p','REVISÃO NECESSÁRIA: uma jornada vinculada mudou. Crie uma correção antes de enviar.','notice'));
        const actions=text('div','','invoice-record-actions');actions.append(recordButton('ABRIR PDF','viewInvoice',r.filename),recordButton('SALVAR CÓPIA','saveInvoicePdf',r.filename));
        if(cancelled)actions.append(recordButton('RESTAURAR','restoreInvoice',r.filename),recordButton('CRIAR CORREÇÃO','correctInvoice',r.filename));else{
          if(!r.needsReview)actions.append(recordButton('↗ ENVIAR AO CLIENTE','shareInvoice',r.filename,'primary send'));
          actions.append(recordButton(r.paid?'MARCAR COMO NÃO PAGO':'✓ MARCAR COMO PAGO','setInvoicePaid',r.filename,'secondary payment-action',{paid:!r.paid}));
          actions.append(recordButton('CORRIGIR / NOVO RASCUNHO','correctInvoice',r.filename),recordButton('REMOVER','cancelInvoice',r.filename,'subtle'));
        }
        card.append(actions);window.FoldUI.record(card,'invoice-'+r.filename);target.append(card);
      }
    }
  }
  window.InvoiceUI={
    selectClient(profile){if(profile.hourly>0)$('invoiceRate').value=profile.hourly;for(const key of ['client','client_address','client_abn','client_email'])document.querySelector('#invoices [name="'+key+'"]').value=profile[key]||'';changed();},
    render(s){state=s;window.Listing.options($('invoices-client'),(s.invoices||[]).map(r=>r.client));window.Listing.options($('invoices-type'),(s.invoices||[]).flatMap(r=>window.Listing.invoiceTypes(r,s)));if(!initialized)populate(s.invoiceDraft||{profile:window.invoiceDefaults.profile,number:s.nextInvoiceNumber||window.invoiceDefaults.nextNumber,rate:window.invoiceDefaults.profile.rate,rows:[]});$('invoiceDate').textContent=today();records();},
    addJourney(s){
      const d=readDraft(),id=String(s.id||s.startedAt);
      if(s.config&&s.config.currency!=='AUD'){error('A jornada usa outra moeda. Preencha manualmente uma tarifa em AUD; o app não converte moedas.');return false;}
      if(s.deleted){error('Restaure a jornada antes de adicioná-la.');return false;}
      const already=(state.invoices||[]).find(r=>r.status!=='cancelled'&&(r.sourceIds||[]).includes(id));
      if(already||d.rows.some(r=>r.sourceId===id)){error('Essa jornada já foi adicionada a um invoice.');return false;}
      const date=s.workDate||new Intl.DateTimeFormat('en-CA',{timeZone:'Australia/Adelaide',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(s.startedAt));
      error('');addRow({date,address:s.worksite||'',hours:(s.workedMs/3600000).toFixed(10).replace(/0+$/,'').replace(/\.$/,''),sourceId:id,sourceFingerprint:s.billingFingerprint||'',workType:s.workType||''});window.FoldUI.expand(document.querySelector('#invoices > .panel.settings-panel'));return true;
    }
  };
  window.invoiceIssued=()=>{window.FoldUI.expand(document.querySelector('#invoices > .panel.settings-panel'));clearTimeout(saveTimer);initialized=false;editing=false;recordsSignature='';error('');};
  $('invoices').addEventListener('input',e=>{if(e.target.matches('input,textarea'))changed();});
  $('invoiceMore').onclick=()=>{limit+=20;recordsSignature='';records();};$('invoiceTrashMore').onclick=()=>{trashLimit+=20;recordsSignature='';records();};
  new IntersectionObserver(entries=>{if(entries.some(e=>e.isIntersecting)&&!$('invoices').classList.contains('hidden')&&!$('invoiceMore').classList.contains('hidden'))$('invoiceMore').click();},{root:document.querySelector('main'),rootMargin:'120px'}).observe($('invoiceMore'));
  $('invoiceRefreshSources').onclick=()=>{clearTimeout(saveTimer);cmd('refreshInvoiceSources',readDraft());};
  $('invoiceAddRow').onclick=()=>addRow();$('invoicePreview').onclick=()=>perform('previewInvoice');$('invoiceIssue').onclick=()=>perform('issueInvoice');$('invoiceClear').onclick=()=>{clearTimeout(saveTimer);cmd('newInvoiceDraft');};
  document.addEventListener('visibilitychange',()=>{if(document.hidden&&editing)save();});
})();
