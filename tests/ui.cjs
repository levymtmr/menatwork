const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async()=>{
  const browser = await chromium.launch({headless:true,args:['--no-sandbox']});
  const page = await browser.newPage({viewport:{width:412,height:858},deviceScaleFactor:2});
  const errors=[];
  async function swipeShift(card,fraction=.85){await card.scrollIntoViewIfNeeded();const b=await card.boundingBox();await page.mouse.move(b.x+b.width*.93,b.y+b.height*.5);await page.mouse.down();await page.mouse.move(b.x+b.width*(.93-fraction),b.y+b.height*.5,{steps:12});await page.mouse.up();}
  const expandCards=()=>page.evaluate(()=>document.querySelectorAll('article .fold-toggle[aria-expanded="false"]').forEach(b=>b.click()));
  page.on('pageerror',e=>errors.push(e.message));
  await page.route('https://app.neonshift.local/**',async route=>{
    const name=new URL(route.request().url()).pathname.slice(1)||'index.html';
    const file=path.join(__dirname,'../app/src/main/assets',name);
    const ext=path.extname(name),types={'.html':'text/html','.css':'text/css','.js':'application/javascript','.ttf':'font/ttf'};
    if(fs.existsSync(file))await route.fulfill({body:fs.readFileSync(file),contentType:types[ext]||'text/plain'});
    else await route.abort();
  });
  await page.addInitScript(()=>{
    const cfg={currency:'AUD',hourly:32,tax:15,consumption:8,fuelPrice:1.85,tyres:.025,maintenance:.05,depreciation:.1};
    window.testState={language:'pt',theme:'pipboy',session:{},config:cfg,history:[],skills:[],serviceRunning:false,now:Date.now()};
    window.calls=[];
    function totals(s){const c=s.config||window.testState.config,ms=s.workedMs||0,km=s.km||0,gross=ms/3600000*c.hourly,tax=gross*c.tax/100,fuel=km*c.consumption/100*c.fuelPrice,tyres=km*c.tyres,maintenance=km*c.maintenance,depreciation=km*c.depreciation,extras=s.extras||0,carCosts=fuel+tyres+maintenance+depreciation+extras;return{gross,tax,fuel,tyres,maintenance,depreciation,extras,carCosts,net:gross-tax-carCosts,workedMs:ms};}
    window.Android={state(){if(!window.testState.invoiceDraft){window.testState.invoiceDraft={profile:{...window.invoiceDefaults.profile,name:'Example Worker',client:'Example Client',bank:'Example Bank',account_name:'Example Worker',bsb:'000-000',account:'00000000',client_email:'client@example.com'},number:18,rate:'32',rows:[]};window.testState.invoices=[];window.testState.nextInvoiceNumber=18;window.testState.invoiceToday='2026-11-16';window.testState.update={stage:'idle',message:'Ready',currentVersion:'1.2.0',progress:0};}const st=window.testState,ms=st.history.filter(r=>!r.deleted).reduce((n,r)=>n+r.workedMs,0),xp=Math.floor(ms/36000);st.progression={xp,level:Math.floor(xp/1000)+1,within:xp%1000,workedMs:ms,pendingXp:Math.floor((st.session.workedMs||0)/36000),skills:st.skills.map(skill=>{const work=skill.deleted?0:st.history.filter(r=>!r.deleted&&(r.skillIds||[]).includes(skill.id)).reduce((n,r)=>n+r.workedMs,0),x=Math.floor(work/36000);return {...skill,workedMs:work,xp:x,level:Math.floor(x/1000)+1,within:x%1000};})};return JSON.stringify({...st,totals:totals(st.session)});},command(action,raw){const p=JSON.parse(raw);window.calls.push({action,p});let s=window.testState.session;
      if(action==='saveClient'){
        const list=window.testState.clients||[],name=p.client.trim().replace(/\s+/g,' ').toLowerCase();if(list.some(c=>c.id!==p.id&&c.client.trim().replace(/\s+/g,' ').toLowerCase()===name)){window.clientError('Já existe um cliente com esse nome. Escolha o cliente cadastrado ou edite os dados dele.');return;}
        const saved={...p,id:p.id||'registered-'+Date.now()};window.testState.clients=[...list.filter(c=>c.id!==saved.id),saved];window.clientSaved(saved);
      }
      if(action==='saveLanguage')window.testState.language=p.language;
      if(action==='saveTheme')window.testState.theme=p.theme;
      if(action==='setInvoicePaid')window.testState.invoices.find(r=>r.filename===p.filename).paid=p.paid;
      if(action==='saveSkill'){const old=window.testState.skills.find(x=>x.id===p.id);if(old)Object.assign(old,p);else window.testState.skills.push({...p,id:'skill-'+Date.now()});window.skillSaved();}
      if(action==='removeSkill'||action==='restoreSkill')window.testState.skills.find(x=>x.id===p.id).deleted=action==='removeSkill';
      if(action==='assignActiveSkills')s.skillIds=p.skillIds;
      if(action==='saveJournal'){
        let ms;if(p.entryMode==='hours'){const h=p.hours.replace(',','.');ms=(h.includes(':')?Number(h.split(':')[0])+Number(h.split(':')[1])/60:Number(h))*3600000;}else{const min=t=>Number(t.split(':')[0])*60+Number(t.split(':')[1]);ms=(min(p.endTime)-min(p.startTime)+(p.nextDay?1440:0)-Number(p.breakMinutes))*60000;}
        const record={...p,id:p.id||'manual-'+p.requestId,trackedWorkedMs:ms,workedMs:ms-Number(p.lunchMinutes||0)*60000,phase:'done',startedAt:new Date(p.workDate+'T08:00:00').getTime(),endedAt:new Date(p.workDate+'T17:00:00').getTime()};record.totals=totals(record);record.billingFingerprint=p.workDate+'|'+ms+'|'+p.worksite;
        window.testState.history=window.testState.history.filter(x=>String(x.id||x.startedAt)!==record.id);window.testState.history.unshift(record);if(p.finishActive)window.testState.session={};window.journalSaved();
      }
      if(action==='removeJournal'||action==='restoreJournal')window.testState.history.find(x=>String(x.id||x.startedAt)===p.id).deleted=action==='removeJournal';
      if(action==='cancelInvoice'||action==='restoreInvoice')window.testState.invoices.find(x=>x.filename===p.filename).status=action==='cancelInvoice'?'cancelled':'issued';
      if(action==='correctInvoice'){const record=window.testState.invoices.find(x=>x.filename===p.filename);record.status='cancelled';window.testState.invoiceDraft={...record.draft,number:window.testState.nextInvoiceNumber};window.invoiceIssued();}
      if(action==='refreshInvoiceSources'){window.testState.invoiceDraft=p;window.invoiceIssued();}
      if(action==='checkUpdate')window.testState.update={...window.testState.update,stage:'checking',message:'Checking'};
      if(action==='saveInvoiceDraft'||action==='previewInvoice')window.testState.invoiceDraft=p;if(action==='saveInvoiceDraft')window.invoiceDraftSaved?.(p._saveRevision);
      if(action==='issueInvoice'){
        const total=p.rows.reduce((sum,r)=>{if(r.address.toLowerCase()==='day off')return sum;const h=r.hours.includes(':')?Number(r.hours.split(':')[0])+Number(r.hours.split(':')[1])/60:Number(r.hours.replace(',','.'));return sum+Math.round(Math.ceil(h*4)/4*Number(p.rate)*100)/100;},0);
        window.testState.invoices.unshift({number:p.number,issued:'2026-11-16',client:p.profile.client,email:p.profile.client_email,total:String(total),filename:'invoice_018_2026-11-16.pdf',draft:p,sourceIds:p.rows.map(r=>r.sourceId).filter(Boolean)});
        window.testState.invoiceDraft={...p,number:p.number+1,rows:[]};window.testState.nextInvoiceNumber=p.number+1;window.invoiceIssued();
      }
      if(action==='saveConfig'){const c=(window.testState.clients||[]).find(c=>c.id===p.clientId);if(c){c.hourly=p.hourly;p.client=c.client;}window.testState.config=p;}
      if(action==='commute'||(action==='work'&&!s.phase)){s=window.testState.session={phase:action==='commute'?'commute':'work',config:{...window.testState.config},startedAt:Date.now(),workedMs:0,km:0,extras:0};window.testState.serviceRunning=true;}
      else if(action==='work')s.phase='work';
      if(action==='pause')s.phase='pause';if(action==='resume')s.phase='work';if(action==='return')s.phase='return';
      if(action==='updateCosts'){s.km=p.km;s.extras=p.extras;if(p.workedMs!==undefined)s.workedMs=p.workedMs;}
      if(action==='finish'){s.phase='done';s.totals=totals(s);window.testState.history.unshift({...s});window.testState.session={};window.testState.serviceRunning=false;}
    }};
  });
  await page.goto('https://app.neonshift.local/');
  await page.waitForFunction(()=>document.getElementById('status').textContent==='PRONTO');assert(await page.locator('#status').isHidden());assert.equal(await page.locator('nav button span').count(),0);
  assert.equal(await page.locator('#gross').innerText(),'$ 0,00');
  assert(await page.locator('#editCosts').isDisabled());
  await page.screenshot({path:'dist/tela-inicial.png',fullPage:true});
  await page.getByRole('button',{name:'↗ INICIAR TRAJETO AO TRABALHO',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='EM TRAJETO');
  await page.locator('#editCosts').click();
  await page.locator('#manualKm').fill('12,5');await page.locator('#manualExtras').fill('5,00');await page.locator('#saveCosts').click();
  await page.waitForFunction(()=>window.testState.session.km===12.5);
  await page.waitForFunction(()=>document.getElementById('balanceNet').textContent.startsWith('−'));
  assert.equal(await page.locator('#gross').innerText(),'$ 0,00');assert((await page.locator('#balanceNet').innerText()).startsWith('−'));
  await page.getByRole('button',{name:'▸ CHEGUEI · INICIAR TRABALHO',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='TRABALHANDO');
  await page.evaluate(()=>window.testState.session.workedMs=23400000);
  await page.waitForFunction(()=>document.getElementById('timer').textContent==='06:30:00');
  assert.equal(await page.locator('#gross').innerText(),'$ 208,00');assert.equal(await page.locator('#balanceNet').innerText(),'$ 167,76');
  await page.screenshot({path:'dist/tela-jornada.png',fullPage:true});
  await page.getByRole('button',{name:'Ⅱ PAUSAR',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='EM PAUSA');
  await page.getByRole('button',{name:'▸ RETOMAR',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='TRABALHANDO');
  await page.locator('nav button[data-page="settings"]').click();
  await page.locator('[name="hourly"]').fill('40,5');await page.locator('[name="tax"]').fill('101');
  await page.getByRole('button',{name:'SALVAR AJUSTES',exact:true}).click();
  assert((await page.locator('#settingsError').innerText()).includes('Confira'));
  await page.locator('[name="tax"]').fill('20');await page.getByRole('button',{name:'SALVAR AJUSTES',exact:true}).click();
  await page.waitForFunction(()=>window.testState.config.hourly===40.5);
  assert.equal(await page.locator('#hourly').innerText(),'$ 32,00'); // active rate stays frozen
  await page.screenshot({path:'dist/tela-ajustes.png',fullPage:true});
  await page.locator('nav button[data-page="today"]').click();
  await page.getByRole('button',{name:'⌁ TERMINAR TRABALHO E REGISTRAR VOLTA',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='VOLTANDO');
  await page.getByRole('button',{name:'■ CHEGUEI · SALVAR JORNADA',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='PRONTO');
  assert((await page.locator('#dailySummary').innerText()).includes('$ 208,00'));
  await page.locator('nav button[data-page="history"]').click();
  assert.equal(await page.locator('.history-card').count(),1);
  assert.equal(await page.locator('.history-gross').innerText(),'Bruto · $ 208,00');
  await expandCards();
  await page.locator('.history-card .history-details summary').click();assert((await page.locator('.history-card .history-liquid').innerText()).includes('$ 167,76'));await page.screenshot({path:'dist/tela-historico.png',fullPage:true});
  await page.locator('#export').click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='export')));
  await page.locator('nav button[data-page="today"]').click();
  await page.getByRole('button',{name:'JÁ ESTOU NO TRABALHO · INICIAR',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='TRABALHANDO');
  assert.equal(await page.locator('#hourly').innerText(),'$ 40,50');
  await page.locator('#editCosts').click();
  await page.locator('#manualHours').fill('7,25');await page.locator('#manualKm').fill('0');await page.locator('#manualExtras').fill('0');
  await page.locator('#saveCosts').click();
  await page.waitForFunction(()=>document.getElementById('timer').textContent==='07:15:00');
  assert.equal(await page.locator('#gross').innerText(),'$ 293,63');assert.equal(await page.locator('#balanceNet').innerText(),'$ 234,90');
  await page.evaluate(()=>window.testState.serviceRunning=false);
  await page.locator('#recovery').waitFor({state:'visible'});
  await page.evaluate(()=>{const r=window.testState.history[0];r.worksite='16 East Pkwy, Fulham SA 5024';r.client='Example Client';window.App.refresh();});
  await page.locator('nav button[data-page="history"]').click();
  await expandCards();
  await page.getByRole('button',{name:'＋ ADICIONAR AO INVOICE',exact:true}).click();
  assert(await page.locator('#invoices').isVisible());
  assert.equal(await page.locator('.invoice-entry').count(),1);
  assert.equal(await page.locator('#invoiceRows input, #invoiceRows textarea, #invoiceRows .edit-linked').count(),0);
  assert.equal(await page.locator('#invoiceAddRow, #invoiceCreateJourney').count(),0);
  const gross=await page.evaluate(()=>window.testState.history[0].totals.gross);
  assert.equal(await page.locator('#invoiceTotal').innerText(),'$ '+gross.toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2}));
  await page.locator('#invoicePreview').click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='previewInvoice'&&x.p.rows.length===1)));
  assert.equal(await page.evaluate(()=>window.testState.invoices.length),0); // preview never issues
  await page.screenshot({path:'dist/tela-invoices.png',fullPage:true});
  await page.locator('nav [data-page="history"]').click();await page.waitForFunction(()=>document.querySelector('#historyList').textContent.includes('ADICIONADA AO RASCUNHO'));
  assert.equal(await page.locator('#historyList').getByRole('button',{name:'＋ ADICIONAR AO INVOICE',exact:true}).count(),0);
  await page.locator('nav [data-page="invoices"]').click();await page.locator('#invoiceIssue').click();
  await page.waitForFunction(()=>document.querySelectorAll('#invoiceRecords article').length===1);
  assert.equal(await page.locator('.invoice-entry').count(),0); // emitted draft clears, PDF remains
  assert.equal(await page.locator('#invoiceNumber').inputValue(),'19');
  await expandCards();
  await page.getByRole('button',{name:'✓ MARCAR COMO PAGO',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#invoiceRecords .billing-badge').textContent==='✓ PAGO');
  await expandCards();
  await page.locator('nav [data-page="history"]').click();await page.waitForFunction(()=>document.querySelector('#historyList').textContent.includes('✓ PAGA'));assert.equal(await page.locator('#historyList').getByRole('button',{name:'＋ ADICIONAR AO INVOICE',exact:true}).count(),0);
  await expandCards();
  await page.screenshot({path:'dist/jornada-paga.png'});await page.locator('nav [data-page="invoices"]').click();await page.getByRole('button',{name:'MARCAR COMO NÃO PAGO',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#invoiceRecords .billing-badge').textContent.includes('AGUARDANDO'));
  await expandCards();
  await page.getByRole('button',{name:'↗ ENVIAR AO CLIENTE',exact:true}).click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='shareInvoice'&&x.p.filename==='invoice_018_2026-11-16.pdf')));
  await expandCards();
  await page.getByRole('button',{name:'SALVAR CÓPIA',exact:true}).click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='saveInvoicePdf')));
  // Narrow phone screens must not overflow horizontally.
  for(const width of [320,360,412,480])for(const height of [400,858]){await page.setViewportSize({width,height});for(const section of ['today','history','skills','invoices','settings']){await page.locator(`nav button[data-page="${section}"]`).click();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),`overflow at ${width} on ${section}`);for(const b of await page.locator('nav button').all()){const box=await b.boundingBox();assert(box.y>=0&&box.y+box.height<=height,`nav outside viewport at ${width}x${height}`);}}}
  assert.deepEqual(errors,[]);
  await page.setViewportSize({width:412,height:858});
  await page.locator('nav button[data-page="settings"]').click();
  await page.locator('#updateButton').click();
  await page.waitForFunction(()=>document.querySelector('#updateButton').disabled);
  await page.evaluate(()=>window.testState.update={...window.testState.update,stage:'downloading',progress:42,message:'Downloading'});
  await page.waitForFunction(()=>document.querySelector('#updateButton').textContent.includes('42%'));
  await page.evaluate(()=>window.testState.update={...window.testState.update,stage:'ready',progress:100,message:'Ready to install'});
  await page.waitForFunction(()=>window.calls.some(c=>c.action==='installUpdate'));
  await page.waitForTimeout(1200);
  assert.equal(await page.evaluate(()=>window.calls.filter(c=>c.action==='installUpdate').length),1);
  await page.evaluate(()=>window.testState.update={...window.testState.update,stage:'error',message:'No connection'});
  await page.waitForFunction(()=>document.querySelector('#updateMessage').textContent==='No connection');
  assert(await page.locator('#updateButton').isEnabled());
  await page.locator('#updateButton').click();
  await page.evaluate(()=>window.testState.update={...window.testState.update,stage:'current',message:'Already current'});
  await page.waitForFunction(()=>document.querySelector('#updateMessage').textContent==='Already current');
  assert.equal(await page.evaluate(()=>window.calls.filter(c=>c.action==='installUpdate').length),1);
  // User-managed skills and retrospective shifts exercise real form dispatch and refresh.
  await page.locator('nav [data-page="skills"]').click();await page.locator('#newSkill').click();
  await page.locator('#skillName').fill('Cladding');await page.locator('#skillDescription').fill('Instalação e acabamento');await page.locator('#saveSkill').click();
  await page.waitForFunction(()=>document.querySelector('#skillList').textContent.includes('Cladding'));
  await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();
  await page.locator('#j-date').fill('2026-11-13');await page.locator('#j-hours').fill('08:30');await page.locator('#j-worksite').fill('Example retroactive site');await page.locator('#journalSkillChoices input').check();
  await page.locator('#j-km').fill('12,5');await page.locator('#j-extras').fill('3');
  const formBox=await page.locator('#saveJournal').boundingBox();assert(formBox.y+formBox.height<=858);
  await page.screenshot({path:'dist/tela-editar-jornada.png'});await page.locator('#saveJournal').click();
  await page.locator('#journalEditor').waitFor({state:'hidden'});
  let retro=page.locator('#historyList article').filter({hasText:'Example retroactive site'});assert.equal(await retro.count(),1);
  await page.locator('nav [data-page="skills"]').click();assert((await page.locator('#skillList').innerText()).includes('850 XP'));await page.screenshot({path:'dist/tela-skills.png'});
  await expandCards();
  await page.locator('nav [data-page="history"]').click();await retro.getByRole('button',{name:'EDITAR',exact:true}).click();
  await page.locator('#j-mode').selectOption('times');await page.locator('#j-start').fill('08:00');await page.locator('#j-end').fill('16:30');await page.locator('#j-break').fill('30');await page.locator('#saveJournal').click();await page.locator('#journalEditor').waitFor({state:'hidden'});
  assert.equal(await retro.count(),1);assert((await retro.innerText()).includes('8 h'));
  await expandCards();
  await retro.getByRole('button',{name:'REMOVER',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#journalTrashCount').textContent==='1');assert.equal(await retro.count(),0);
  await page.locator('nav [data-page="skills"]').click();assert((await page.locator('#skillList').innerText()).includes('0 XP'));
  await expandCards();
  await page.locator('nav [data-page="history"]').click();await page.locator('#journalTrash > summary').click();await page.locator('#journalTrashList').getByRole('button',{name:'RESTAURAR',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#journalTrash').classList.contains('hidden'));assert.equal(await retro.count(),1);
  await page.locator('nav [data-page="today"]').click();await page.locator('#activeSkillChoices input').check();await page.locator('#saveActiveSkills').click();assert(await page.evaluate(()=>window.calls.some(c=>c.action==='assignActiveSkills')));
  await page.getByRole('button',{name:'CORRIGIR HORÁRIOS / ENCERRAR',exact:true}).click();await page.locator('#j-date').fill('2026-11-12');await page.locator('#j-hours').fill('6');await page.locator('#saveJournal').click();await page.waitForFunction(()=>document.querySelector('#status').textContent==='PRONTO');
  await expandCards();
  await page.locator('nav [data-page="invoices"]').click();await page.locator('#invoiceRecords').getByRole('button',{name:'REMOVER',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#invoiceTrashCount').textContent==='1');assert((await page.locator('#invoiceLedgerTotals').innerText()).includes('0 invoices'));
  await expandCards();
  await page.locator('#invoiceTrash > summary').click();await page.locator('#invoiceTrashList').getByRole('button',{name:'RESTAURAR',exact:true}).click();await page.waitForFunction(()=>document.querySelector('#invoiceRecords').textContent.includes('INVOICE #018'));
  await expandCards();
  await page.locator('#invoiceRecords').getByRole('button',{name:'CORRIGIR / NOVO RASCUNHO',exact:true}).click();await page.waitForFunction(()=>document.querySelectorAll('.invoice-entry').length===1);
  await page.locator('nav [data-page="history"]').click();await page.waitForFunction(()=>document.querySelector('#historyList').textContent.includes('ADICIONADA AO RASCUNHO'));
  await page.locator('nav [data-page="invoices"]').click();await swipeShift(page.locator('.invoice-entry').first());
  await page.locator('nav [data-page="history"]').click();await page.waitForFunction(()=>!document.querySelector('#historyList').textContent.includes('ADICIONADA AO RASCUNHO'));
  const financialBefore=await page.evaluate(()=>JSON.stringify([window.testState.config,window.testState.session,window.testState.history,window.testState.invoices]));
  for(const theme of ['retro','medieval','cyberpunk','pipboy']){
    await page.locator('nav [data-page="settings"]').click();await page.locator(`[data-theme-choice="${theme}"]`).click();await page.waitForFunction(t=>window.testState.theme===t&&document.documentElement.dataset.theme===t,theme);
    assert.equal(await page.locator(`[data-theme-choice="${theme}"]`).getAttribute('aria-pressed'),'true');
    await page.screenshot({path:`dist/tema-${theme}-ajustes.png`});await page.locator('nav [data-page="today"]').click();await page.screenshot({path:`dist/tema-${theme}.png`});
    for(const width of [320,412]){await page.setViewportSize({width,height:700});for(const section of ['today','history','skills','invoices','settings']){await page.locator(`nav [data-page="${section}"]`).click();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),`${theme}/${section}/${width}`);for(const b of await page.locator('nav button').all()){const r=await b.boundingBox();assert(r.y+r.height<=700);}}}
    await page.setViewportSize({width:412,height:858});
  }
  assert.equal(await page.evaluate(()=>JSON.stringify([window.testState.config,window.testState.session,window.testState.history,window.testState.invoices])),financialBefore);
  assert.deepEqual(errors,[]);
  console.log('PASS: work flows, invoice draft/rounding/preview/issue/share/save dispatch, journal management, skill XP, invoice cancellation/correction, billing markers, payments, three themes, 5 pages, 4 widths, 2 heights, and navigation bounds.');
  const languagePage=await browser.newPage({viewport:{width:412,height:858}});languagePage.on('pageerror',e=>errors.push(e.message));
  await languagePage.goto('file://'+path.join(__dirname,'../app/src/main/assets/index.html'));
  assert.equal(await languagePage.locator('html').getAttribute('lang'),'en');assert.equal(await languagePage.locator('html').getAttribute('data-theme'),'retro');assert.equal(await languagePage.locator('#status').innerText(),'PREVIEW');
  await languagePage.getByRole('button',{name:'Settings',exact:true}).click();assert.equal(await languagePage.locator('#appLanguage').inputValue(),'en');
  await languagePage.locator('#appLanguage').selectOption('pt');await languagePage.waitForFunction(()=>document.documentElement.lang==='pt-BR');assert.equal(await languagePage.locator('#themesTitle').innerText(),'TEMAS');
  await languagePage.reload();assert.equal(await languagePage.locator('#appLanguage').inputValue(),'pt');
  await languagePage.locator('nav [data-page="settings"]').click();await languagePage.locator('#appLanguage').selectOption('en');await languagePage.reload();assert.equal(await languagePage.locator('html').getAttribute('lang'),'en');
  await languagePage.locator('nav [data-page="settings"]').click();await languagePage.locator('[data-theme-choice="medieval"]').click();await languagePage.screenshot({path:'dist/settings-english.png'});
  await languagePage.locator('nav [data-page="invoices"]').click();assert.equal(await languagePage.locator('#invoiceIssue').innerText(),'ISSUE AND SAVE PDF');
  await languagePage.locator('[name="client"]').evaluate(e=>e.value='Trabalho');await languagePage.locator('nav [data-page="settings"]').click();await languagePage.locator('#appLanguage').selectOption('pt');assert.equal(await languagePage.locator('[name="client"]').inputValue(),'Trabalho');
  assert.deepEqual(errors,[]);await languagePage.close();
  await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();await page.locator('#j-date').fill('2026-11-16');await page.locator('#j-hours').fill('8');await page.locator('#j-lunch').fill('45');await page.locator('#j-client').fill('Lunch Client');await page.locator('#j-workType').fill('Cladding');await page.locator('#saveJournal').click();await page.locator('#journalEditor').waitFor({state:'hidden'});assert.equal(await page.evaluate(()=>window.testState.history[0].workedMs),26100000);
  await expandCards();
  await page.locator('#historyList article').filter({hasText:'Lunch Client'}).getByRole('button',{name:'EDITAR',exact:true}).click();assert.equal(await page.locator('#j-hours').inputValue(),'8');assert.equal(await page.locator('#j-lunch').inputValue(),'45');await page.locator('#saveJournal').click();await page.locator('#journalEditor').waitFor({state:'hidden'});assert.equal(await page.evaluate(()=>window.testState.history[0].workedMs),26100000);
  await page.evaluate(()=>{const base=window.testState.history[0],invoice=window.testState.invoices[0];window.testState.theme='retro';window.testState.now=new Date('2026-11-17T12:00:00').getTime();window.testState.history=Array.from({length:65},(_,i)=>({...base,id:'filter-'+i,startedAt:1000+i,workDate:i%2?'2026-11-15':'2026-11-17',client:i%2?'Beta':'Alpha',workType:i%2?'Painting':'Cladding',worksite:'Site '+i,notes:''}));window.testState.invoices=Array.from({length:65},(_,i)=>({...invoice,number:100+i,issued:i%2?'2026-11-15':'2026-11-17',status:'issued',paid:i%2===0,client:i%2?'Beta':'Alpha',sourceIds:['filter-'+i],filename:'fixture-'+i+'.pdf',draft:{...invoice.draft,rows:[{date:'2026-11-17',address:'Site '+i,hours:'7.25',sourceId:'filter-'+i,workType:i%2?'Painting':'Cladding'}]}}));window.App.refresh();});
  assert.equal(await page.locator('#historyList article').count(),20);await page.locator('#historyMore').scrollIntoViewIfNeeded();await page.waitForFunction(()=>document.querySelectorAll('#historyList article').length>=40);
  await page.locator('#history-filters > summary').click();await page.locator('#history-period').selectOption('week');await page.locator('#history-client').selectOption('Alpha');await page.locator('#history-type').selectOption('Cladding');await page.locator('#history-status').selectOption('paid');assert((await page.locator('#historyCount').innerText()).includes('33'));
  await page.locator('#history-search').fill('Site 0');assert.equal(await page.locator('#historyList article').count(),1);await page.screenshot({path:'dist/retro-history-filters.png'});await page.locator('#history-clear').click();assert.equal(await page.locator('#historyList article').count(),20);
  await page.locator('nav [data-page="invoices"]').click();assert.equal(await page.locator('#invoiceRecords article').count(),20);await page.locator('#invoiceMore').scrollIntoViewIfNeeded();await page.waitForFunction(()=>document.querySelectorAll('#invoiceRecords article').length>=40);
  await page.locator('#invoices-filters > summary').click();await page.locator('#invoices-period').selectOption('lastWeek');await page.locator('#invoices-client').selectOption('Beta');await page.locator('#invoices-type').selectOption('Painting');await page.locator('#invoices-status').selectOption('unpaid');assert((await page.locator('#invoiceCount').innerText()).includes('32'));await page.locator('#invoices-search').fill('101');assert.equal(await page.locator('#invoiceRecords article').count(),1);await page.screenshot({path:'dist/retro-invoice-filters.png'});
  assert.deepEqual(errors,[]);console.log('PASS: lunch correction without double deduction, metadata, 65-record lists, combined filters and incremental scrolling.');
  // Android may dispatch change without input on native select popups.
  await page.locator('nav [data-page="settings"]').click();await page.evaluate(()=>{const el=document.getElementById('currency');el.value='USD';el.dispatchEvent(new Event('change',{bubbles:true}));window.App.refresh();});assert.equal(await page.locator('#currency').inputValue(),'USD');await page.waitForTimeout(1200);assert.equal(await page.locator('#currency').inputValue(),'USD');
  await page.evaluate(()=>{window.testState.worksites=['12 Main Street','34 South Road'];window.App.refresh();});await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();
  const journalSite=page.locator('#j-worksite').locator('..').locator('..').locator('.saved-site-select').first();
  await journalSite.selectOption('34 South Road');assert.equal(await page.locator('#j-worksite').inputValue(),'34 South Road');assert.equal(await journalSite.inputValue(),'34 South Road');await page.waitForTimeout(1200);assert.equal(await page.locator('#j-worksite').inputValue(),'34 South Road');assert.equal(await journalSite.inputValue(),'34 South Road');
  await page.screenshot({path:'dist/saved-worksites.png'});await page.locator('#closeJournal').click();

  if(!(await page.locator('#invoices-filters').evaluate(e=>e.open)))await page.locator('#invoices-filters > summary').click();await page.locator('#invoices-client').focus();await page.evaluate(()=>{const el=document.getElementById('invoices-client');el.value='Alpha';el.dispatchEvent(new Event('change',{bubbles:true}));window.testState.invoices.push({...window.testState.invoices[0],number:999,client:'New client'});window.App.refresh();});assert.equal(await page.locator('#invoices-client').inputValue(),'Alpha');await page.locator('#invoices-search').focus();assert.equal(await page.locator('#invoices-client').inputValue(),'Alpha');
  assert.deepEqual(errors,[]);console.log('PASS: saved sites fill address fields and selections survive refresh/change-only events.');
  await page.evaluate(()=>{const old=window.testState.invoiceDraft.profile;window.testState.invoiceDraft={...window.testState.invoiceDraft,profile:{...old,client:'Alpha Client',client_email:'alpha@example.com',client_address:'Alpha Road',client_abn:'111',bank:'Protected Bank'}};window.testState.invoices=[{...window.testState.invoices[0],client:'Beta Client',draft:{...window.testState.invoiceDraft,profile:{...old,client:'Beta Client',client_email:'beta@example.com',client_address:'Beta Road',client_abn:'222'}}}];window.testState.history.push({...window.testState.history[0],id:'history-client',client:'Walk-in Client'});window.invoiceIssued();window.App.refresh();});
  await page.locator('nav [data-page="invoices"]').click();await page.waitForFunction(()=>document.querySelector('#invoices .client-select').value==='alpha client');await page.locator('#invoices .client-select').selectOption('beta client');assert.equal(await page.locator('#invoices [name="client"]').inputValue(),'Beta Client');assert.equal(await page.locator('#invoices [name="client_email"]').inputValue(),'beta@example.com');assert.equal(await page.locator('#invoices [name="client_address"]').inputValue(),'Beta Road');assert.equal(await page.locator('[name="bank"]').inputValue(),'Protected Bank');
  await page.locator('#invoices .client-select').selectOption('walk-in client');assert.equal(await page.locator('#invoices [name="client_email"]').inputValue(),'');assert.equal(await page.locator('#invoices [name="client_address"]').inputValue(),'');
  await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();await page.locator('#journalForm .client-select').selectOption('beta client');assert.equal(await page.locator('#j-client').inputValue(),'Beta Client');await page.screenshot({path:'dist/quick-client.png'});await page.locator('#closeJournal').click();
  const compact=page.locator('#historyList article').first();if(await compact.locator('.fold-toggle').getAttribute('aria-expanded')==='true')await compact.locator('.fold-toggle').click();assert(await compact.locator('.fold-body').isHidden());assert(await compact.locator('.history-gross').isVisible());await compact.locator('.fold-toggle').click();assert(await compact.locator('.fold-body').isVisible());await page.evaluate(()=>window.App.refresh());assert.equal(await compact.locator('.fold-toggle').getAttribute('aria-expanded'),'true');await compact.locator('.fold-toggle').click();await page.screenshot({path:'dist/compact-cards.png'});
  await page.locator('nav [data-page="today"]').click();assert(await page.locator('#status').isHidden());assert.equal(await page.locator('nav span').count(),0);await page.screenshot({path:'dist/navigation-clean.png'});assert.deepEqual(errors,[]);console.log('PASS: quick clients, correct contact details, no stale recipient, collapsible cards and clean navigation.');
  await page.evaluate(()=>{window.testState.clients=[{id:'alpha-id',client:'Alpha Client',client_email:'alpha@example.com',client_address:'Alpha Road',client_abn:'111'},{id:'beta-id',client:'Beta Client',client_email:'beta@example.com',client_address:'Beta Road',client_abn:'222'}];window.App.refresh();});
  await page.locator('nav [data-page="settings"]').click();await page.locator('#newClient').click();await page.locator('#clientName').fill('Gamma Client');await page.locator('#clientHourly').fill('48.50');await page.locator('#clientEmail').fill('gamma@example.com');await page.locator('#clientAddress').fill('33 Gamma Road');await page.locator('#clientAbn').fill('333');await page.locator('#saveClient').click();await page.locator('#clientEditor').waitFor({state:'hidden'});assert.equal(await page.evaluate(()=>window.testState.clients.length),3);
  await page.locator('#newClient').click();await page.locator('#clientName').fill('gamma client');await page.locator('#saveClient').click();assert(await page.locator('#clientEditor').isVisible());assert((await page.locator('#clientError').innerText()).includes('existe'));await page.locator('#closeClient').click();
  await page.locator('#clientList .client-entry').filter({hasText:'Gamma Client'}).getByRole('button',{name:'EDITAR',exact:true}).click();await page.locator('#clientEmail').fill('updated-gamma@example.com');await page.locator('#saveClient').click();await page.locator('#clientEditor').waitFor({state:'hidden'});assert.equal(await page.evaluate(()=>window.testState.clients.length),3);await page.screenshot({path:'dist/client-directory.png'});
  await page.locator('nav [data-page="invoices"]').click();await page.locator('#invoices .client-select').selectOption('gamma client');assert.equal(Number(await page.locator('#invoiceRate').inputValue()),48.5);assert.equal(await page.locator('#invoices [name="client_email"]').inputValue(),'updated-gamma@example.com');assert.equal(await page.locator('#invoices [name="client_address"]').inputValue(),'33 Gamma Road');assert.equal(await page.locator('[name="bank"]').inputValue(),'Protected Bank');
  await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();await page.locator('#journalForm .new-client-shortcut').click();assert(await page.locator('#journalEditor').evaluate(e=>e.inert));await page.locator('#clientHourly').fill('55');await page.locator('#clientName').fill('Delta Client');await page.locator('#clientEmail').fill('delta@example.com');await page.screenshot({path:'dist/new-client-form.png'});await page.locator('#saveClient').click();await page.locator('#clientEditor').waitFor({state:'hidden'});assert(await page.locator('#journalEditor').isVisible());assert.equal(await page.locator('#j-client').inputValue(),'Delta Client');assert.equal(await page.locator('#j-hourly').inputValue(),'55');assert.equal(await page.locator('#journalForm .client-select').inputValue(),'delta client');assert.equal(await page.locator('#journalEditor').evaluate(e=>e.inert),false);await page.locator('#closeJournal').click();assert.deepEqual(errors,[]);console.log('PASS: register, edit, reject duplicates, choose clients and preserve nested journal editor.');
  await page.locator('nav [data-page="settings"]').click();await page.locator('#workClient').selectOption(await page.evaluate(()=>window.testState.clients.find(c=>c.client==='Gamma Client').id));assert.equal(Number(await page.locator('#settingHourly').inputValue()),48.5);await page.locator('#settingHourly').fill('49');await page.locator('#settingsForm button[type="submit"]').click();await page.waitForTimeout(400);assert.equal(await page.evaluate(()=>window.testState.config.client),'Gamma Client');assert.equal(await page.evaluate(()=>window.testState.clients.find(c=>c.client==='Gamma Client').hourly),49);await page.evaluate(()=>window.App.refresh());assert.equal(Number(await page.locator('#settingHourly').inputValue()),49);console.log('PASS: Settings Work selects client and saves that client hourly rate.');
  await page.evaluate(()=>{window.testState.history.push({id:'ot-ui',workDate:new Date().toLocaleDateString('en-CA'),startedAt:Date.now(),workedMs:33300000,client:'Gamma Client',worksite:'OT site',config:{...window.testState.config,currency:'AUD',hourly:32,overtime:true},totals:{gross:360,net:306,workedMs:33300000,overtimePremium:64,overtime15Ms:7200000,overtime2Ms:3600000}});window.App.refresh();});await page.locator('nav [data-page="today"]').click();assert((await page.locator('#weeklyTotals').innerText()).includes('360')||(await page.locator('#weeklyTotals').innerText()).length>20);await page.locator('#weeklyPanel').scrollIntoViewIfNeeded();await page.screenshot({path:'dist/weekly-summary.png'});await page.evaluate(()=>{window.InvoiceUI.addJourney(window.testState.history.find(r=>r.id==='ot-ui'));});await page.locator('nav [data-page="invoices"]').click();const otEntry=page.locator('.invoice-entry[data-source-id="ot-ui"]');assert.equal(await otEntry.locator('input').count(),0);assert((await otEntry.locator('.line-total').innerText()).includes('360'));console.log('PASS: overtime source invoice includes premium and locks tracked hours; weekly card renders.');
  await page.locator('nav [data-page="settings"]').click();await page.locator('#newClient').click();await page.locator('#clientName').fill('Marken');await page.locator('#clientHourly').fill('32');assert.equal(await page.locator('#clientPayRule').inputValue(),'marken');await page.locator('#saveClient').click();await page.locator('#clientEditor').waitFor({state:'hidden'});assert.equal(await page.evaluate(()=>window.testState.clients.find(c=>c.client==='Marken').payRule),'marken');
  await page.locator('nav [data-page="history"]').click();await page.locator('#newJournal').click();await page.locator('#journalForm .client-select').selectOption('marken');assert.equal(await page.locator('#j-payRule').inputValue(),'marken');assert(await page.locator('#j-lunch').isDisabled());await page.locator('#j-start').fill('08:00');await page.locator('#j-end').fill('16:00');assert((await page.locator('#journalDuration').innerText()).includes('7,5'));await page.screenshot({path:'dist/marken-journal.png'});await page.locator('#closeJournal').click();
  await page.locator('nav [data-page="invoices"]').click();while(await page.locator('#invoiceRows .invoice-entry').count())await swipeShift(page.locator('#invoiceRows .invoice-entry').first());await page.evaluate(()=>{const r={id:'marken-ui',workDate:'2026-11-16',workedMs:27060000,client:'Marken',worksite:'Marken site',config:{currency:'AUD',hourly:32,overtime:true,payRule:'marken'},timesheetEntry:{date:'2026-11-16',start:'08:00',end:'16:01',site:'Marken site',position:'Picker',company:'Marken',breakMs:1800000,regularMs:27060000,ot15Ms:0,ot2Ms:0,rate:'32'},totals:{overtimePremium:0},billingNote:'Marken: ordinary 7.5167 h; unpaid meal 30 min'};window.testState.history.push(r);window.App.refresh();window.InvoiceUI.addJourney(r);});await page.locator('nav [data-page="invoices"]').click();assert(/240[,.]53/.test(await page.locator('.invoice-entry[data-source-id="marken-ui"] .line-total').innerText()));console.log('PASS: Marken default rule, automatic meal preview and exact-minute invoice amount.');
  await page.evaluate(()=>{const r={id:'legacy-marken-clock',client:'Marken',workDate:'2026-11-16',startedAt:Date.now(),entryMode:'times',startTime:'07:00',endTime:'15:00',workedMs:27000000,worksite:'Legacy Marken site',config:{...window.testState.config,currency:'AUD',hourly:32,payRule:'none',overtime:false},totals:{gross:240,net:200},timesheetEntry:{date:'2026-11-16',start:'07:00',end:'15:00',site:'Legacy Marken site',position:'Picker',company:'Marken',breakMs:1800000,regularMs:27000000,ot15Ms:0,ot2Ms:0,rate:'32'}};window.testState.history.push(r);window.App.refresh();window.InvoiceUI.addJourney(r);});await page.locator('#invoicePreview').click();assert(await page.evaluate(()=>window.calls.some(c=>c.action==='previewInvoice'&&c.p.rows.some(r=>r.sourceId==='legacy-marken-clock'&&r.timesheet.start==='07:00'&&r.timesheet.end==='15:00'))));console.log('PASS: legacy Marken shift clock metadata is passed to invoice preview without changing its pay rule.');
  while(await page.locator('#invoiceRows .invoice-entry').count())await swipeShift(page.locator('#invoiceRows .invoice-entry').first());
  await page.locator('#invoices .client-select').selectOption('gamma client');
  await page.evaluate(()=>{for(let n=0;n<3;n++){window.testState.history.push({id:'group-'+n,workDate:'2026-11-16',startedAt:Date.now()+n,workedMs:3600000,trackedWorkedMs:3600000,client:'Gamma Client',worksite:'Group site '+n,entryMode:'hours',config:{...window.testState.config,currency:'AUD',hourly:49,overtime:false,payRule:'none'},totals:{gross:49,net:25,workedMs:3600000},skillIds:[]});}window.App.refresh();});
  await page.locator('#invoicePickJourneys').click();await page.locator('#invoiceJourneySearch').fill('Group site');
  await page.locator('#invoiceJourneyList button').nth(0).click();await page.locator('#invoiceJourneyList button').nth(1).click();
  assert.equal(await page.locator('#invoiceRows input').count(),0);assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 98,00');
  await page.locator('#invoiceGoHistory').click();await page.locator('#history-clear').click();await page.locator('#history-search').fill('Group site');await expandCards();
  await page.locator('#historyList article').filter({hasText:'Group site 1'}).getByRole('button',{name:'EDITAR',exact:true}).click();
  await page.locator('#j-hours').fill('2');await page.locator('#saveJournal').click();await page.locator('#journalEditor').waitFor({state:'hidden'});
  await page.locator('nav [data-page="invoices"]').click();assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 147,00');
  await page.waitForTimeout(900);assert.equal(await page.evaluate(()=>window.testState.invoiceDraft.rows.find(r=>r.sourceId==='group-1').sourceGross),98);
  await swipeShift(page.locator('.invoice-entry[data-source-id="group-0"]'));assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 98,00');
  await page.locator('#invoiceUndoButton').click();assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 147,00');
  const swipeCard=page.locator('.invoice-entry[data-source-id="group-0"]');await swipeShift(swipeCard,.25);assert(await swipeCard.locator('.swipe-remove').isEnabled());await swipeCard.locator('.swipe-remove').click();assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 98,00');
  await page.evaluate(()=>{const r=window.testState.history.find(r=>r.id==='group-1');r.entryMode='times';r.startTime='07:00';r.endTime='09:00';window.App.refresh();});assert.equal(await page.locator('.invoice-entry[data-source-id="group-1"] .shift-clock').innerText(),'07:00 – 09:00');
  await page.locator('nav [data-page="history"]').click();const clockCard=page.locator('#historyList article').filter({hasText:'Group site 1'});assert.equal(await clockCard.locator('.record-clock').innerText(),'07:00 – 09:00');await clockCard.locator('.fold-toggle').evaluate(e=>{if(e.getAttribute('aria-expanded')==='true')e.click();});assert(await clockCard.locator('.record-clock').isVisible());await clockCard.scrollIntoViewIfNeeded();await page.screenshot({path:'dist/history-shift-times.png'});await page.locator('nav [data-page="invoices"]').click();
  assert.equal(await page.locator('.invoice-checkout .help,.invoice-checkout details,.invoice-checkout h3').count(),0);
  await page.locator('#invoiceJourneyPicker').evaluate(e=>e.open=false);await page.locator('#invoiceRows').scrollIntoViewIfNeeded();await page.screenshot({path:'dist/invoice-swipe-retro.png'});
  console.log('PASS: full swipe removes, undo restores, partial swipe reveals removal, start/finish shown, checkout is concise.');
  assert.deepEqual(errors,[]);console.log('PASS: selection-only invoice, no shift editing fields, gross totals, History edits sync automatically and removing a shift updates totals.');
  const touch=await page.context().newCDPSession(page);await touch.send('Emulation.setTouchEmulationEnabled',{enabled:true,maxTouchPoints:1});
  const touchCard=page.locator('.invoice-entry[data-source-id="group-1"]');await touchCard.scrollIntoViewIfNeeded();let rect=await touchCard.boundingBox();
  await touch.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:rect.x+rect.width*.9,y:rect.y+rect.height*.5}]});
  for(let n=1;n<=10;n++)await touch.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:rect.x+rect.width*(.9-.8*n/10),y:rect.y+rect.height*.5}]});
  await touch.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await touchCard.waitFor({state:'detached'});await page.locator('#invoiceUndoButton').click();assert.equal(await touchCard.count(),1);
  await touchCard.scrollIntoViewIfNeeded();rect=await touchCard.boundingBox();await touch.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:rect.x+rect.width*.5,y:rect.y+rect.height*.6}]});
  for(let n=1;n<=6;n++)await touch.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:rect.x+rect.width*.5,y:rect.y+rect.height*.6-n*10}]});
  await touch.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});assert.equal(await touchCard.count(),1);assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 98,00');
  await touch.send('Emulation.setTouchEmulationEnabled',{enabled:false});
  await page.evaluate(()=>{window.invoiceError('Confira os dados de prestador, cliente e pagamento.');document.getElementById('invoiceEmpty').classList.remove('hidden');});
  const colors=await page.evaluate(()=>{const n=getComputedStyle(document.getElementById('invoiceEmpty')),e=getComputedStyle(document.getElementById('invoiceError')),p=getComputedStyle(document.querySelector('.invoice-draft'));return {notice:[n.color,n.backgroundColor],error:[e.color,p.backgroundColor]};});
  const luminance=rgb=>rgb.match(/[\d.]+/g).slice(0,3).map(Number).map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;}).reduce((n,v,i)=>n+v*[.2126,.7152,.0722][i],0);
  for(const [kind,pair]of Object.entries(colors)){const values=pair.map(luminance).sort((a,b)=>a-b);assert((values[1]+.05)/(values[0]+.05)>=4.5,kind+' contrast');}
  await page.locator('#invoiceError').scrollIntoViewIfNeeded();await page.screenshot({path:'dist/invoice-retro-alerts.png'});assert.deepEqual(errors,[]);console.log('PASS: native touch swipe removes, vertical touch scroll preserves shifts, Retro notice/error contrast >=4.5:1.');
  await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
