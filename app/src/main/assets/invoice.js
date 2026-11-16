'use strict';
(() => {
  const $=id=>document.getElementById(id), native=typeof Android!=='undefined';
  let state,initialized=false,saveTimer,recordsSignature='',editing=false;
  const fields={
    name:'Nome do prestador',address:'Endereço do prestador',email:'E-mail do prestador',abn:'ABN do prestador',
    bank:'Banco',account_name:'Nome da conta',bsb:'BSB',account:'Número da conta',
    client:'Nome do cliente',client_address:'Endereço do cliente',client_abn:'ABN do cliente',client_email:'E-mail do cliente',description:'Descrição dos serviços (em inglês)'
  };
  function text(tag,value,cls){const e=document.createElement(tag);e.textContent=value;if(cls)e.className=cls;return e;}
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
  const cash=cents=>'$ '+(cents/100).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2});
  function input(label,name,value,parent,kind='text'){
    const wrap=text('label',label),el=document.createElement(kind==='textarea'?'textarea':'input');
    if(kind!=='textarea')el.type=kind;el.name=name;el.value=value||'';el.maxLength=name.includes('address')?350:name==='description'?500:name.includes('email')?160:name.includes('abn')||name==='bsb'?40:name==='account'?80:120;
    wrap.append(el);parent.append(wrap);return el;
  }
  for(const [key,label]of Object.entries(fields))input(label,key,'',$((key.startsWith('client')||key==='description')?'clientFields':'sellerFields'),key.includes('address')||key==='description'?'textarea':key.includes('email')?'email':'text');
  function readDraft(){
    const profile={};for(const key of Object.keys(fields))profile[key]=document.querySelector(`[name="${key}"]`).value.trim();
    const rows=[...document.querySelectorAll('.invoice-entry')].map(el=>({date:el.querySelector('[name="date"]').value,address:el.querySelector('[name="workAddress"]').value.trim(),hours:el.querySelector('[name="hours"]').value.trim()||'0',sourceId:el.dataset.sourceId||''}));
    return {number:Number($('invoiceNumber').value),rate:$('invoiceRate').value.trim().replace(',','.'),profile,rows};
  }
  function updateTotals(){
    const draft=readDraft();let actual=0,quarters=0,cents=0,invalid=false;
    draft.rows.forEach((r,i)=>{const target=$('invoiceRows').children[i].querySelector('.line-total');try{const l=line(r,draft.rate);actual+=l.actual;quarters+=l.quarters;cents+=l.cents;target.textContent=l.dayOff?'Day off · '+cash(0):l.quarters/4+' h cobradas · '+cash(l.cents);}catch(e){target.textContent=e.message;invalid=true;}});
    $('invoiceActual').textContent=actual.toLocaleString('pt-BR',{maximumFractionDigits:4})+' h';$('invoiceBilled').textContent=(quarters/4).toLocaleString('pt-BR',{maximumFractionDigits:2})+' h';$('invoiceTotal').textContent=invalid?'Confira as linhas':cash(cents);
  }
  function save(){clearTimeout(saveTimer);if(native&&initialized)cmd('saveInvoiceDraft',readDraft());}
  function changed(){editing=true;updateTotals();clearTimeout(saveTimer);saveTimer=setTimeout(save,500);}
  function addRow(row={date:today(),address:'',hours:'0',sourceId:''},persist=true){
    const box=text('div','','invoice-entry');box.dataset.sourceId=row.sourceId||'';
    const top=text('div','','entry-top');top.append(text('span',row.sourceId?'JORNADA DO HISTÓRICO':'LANÇAMENTO MANUAL'));
    const remove=text('button','×','remove-entry');remove.type='button';remove.setAttribute('aria-label','Remover lançamento');remove.onclick=()=>{box.remove();changed();};top.append(remove);box.append(top);
    const grid=text('div','','invoice-grid');input('Data','date',row.date,grid,'date');const h=input('Horas reais','hours',row.hours,grid);h.inputMode='decimal';h.maxLength=32;box.append(grid);
    const addr=input('Local do trabalho','workAddress',row.address,box);addr.maxLength=1000;addr.setAttribute('list','worksites');
    box.append(text('p','','line-total'));$('invoiceRows').append(box);if(persist)changed();
  }
  function populate(draft){
    $('invoiceNumber').value=draft.number||state.nextInvoiceNumber||18;$('invoiceRate').value=String(draft.rate||'32').replace('.',',');
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
  function recordButton(label,action,filename,cls='secondary'){const b=text('button',label,'button '+cls);b.onclick=()=>cmd(action,{filename});return b;}
  function records(){
    const list=state.invoices||[],signature=JSON.stringify(list);if(signature===recordsSignature)return;recordsSignature=signature;
    $('invoiceRecords').replaceChildren();
    if(!list.length){$('invoiceRecords').append(text('p','Seus PDFs emitidos neste celular aparecerão aqui.','empty'));return;}
    for(const r of list){
      const card=text('article','','panel history-card');card.append(text('h3','INVOICE #'+String(r.number).padStart(3,'0')),text('p',r.client,'muted'),text('p',cash(Math.round(Number(r.total)*100)),'history-net'),text('p','Emissão: '+r.issued+' · AUD','muted'));
      const actions=text('div','','invoice-record-actions');actions.append(recordButton('ABRIR PDF','viewInvoice',r.filename),recordButton('SALVAR CÓPIA','saveInvoicePdf',r.filename),recordButton('↗ ENVIAR AO CLIENTE','shareInvoice',r.filename,'primary send'));card.append(actions);$('invoiceRecords').append(card);
    }
  }
  window.InvoiceUI={
    render(s){state=s;if(!initialized)populate(s.invoiceDraft||{profile:window.invoiceDefaults.profile,number:s.nextInvoiceNumber||window.invoiceDefaults.nextNumber,rate:window.invoiceDefaults.profile.rate,rows:[]});$('invoiceDate').textContent=today();const data=s.invoiceAddresses||window.invoiceDefaults.addresses;if(!$('worksites').children.length){for(const value of [...data,'Day off']){const o=document.createElement('option');o.value=value;$('worksites').append(o);}}records();},
    addJourney(s){
      const d=readDraft(),id=String(s.id||s.startedAt);
      if(s.config&&s.config.currency!=='AUD'){error('A jornada usa outra moeda. Preencha manualmente uma tarifa em AUD; o app não converte moedas.');return false;}
      const already=(state.invoices||[]).find(r=>(r.sourceIds||[]).includes(id));
      if(already||d.rows.some(r=>r.sourceId===id)){error('Essa jornada já foi adicionada a um invoice.');return false;}
      const date=new Intl.DateTimeFormat('en-CA',{timeZone:'Australia/Adelaide',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(s.startedAt));
      error('');addRow({date,address:s.worksite||'',hours:(s.workedMs/3600000).toFixed(10).replace(/0+$/,'').replace(/\.$/,''),sourceId:id});return true;
    }
  };
  window.invoiceIssued=()=>{clearTimeout(saveTimer);initialized=false;editing=false;recordsSignature='';error('');};
  $('invoices').addEventListener('input',e=>{if(e.target.matches('input,textarea'))changed();});
  $('invoiceAddRow').onclick=()=>addRow();$('invoicePreview').onclick=()=>perform('previewInvoice');$('invoiceIssue').onclick=()=>perform('issueInvoice');$('invoiceClear').onclick=()=>{clearTimeout(saveTimer);cmd('newInvoiceDraft');};
  document.addEventListener('visibilitychange',()=>{if(document.hidden&&editing)save();});
})();
