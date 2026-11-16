const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async()=>{
  const browser = await chromium.launch({headless:true,args:['--no-sandbox']});
  const page = await browser.newPage({viewport:{width:412,height:858},deviceScaleFactor:2});
  const errors=[];
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
    window.testState={session:{},config:cfg,history:[],serviceRunning:false,now:Date.now()};
    window.calls=[];
    function totals(s){const c=s.config||window.testState.config,ms=s.workedMs||0,km=s.km||0,gross=ms/3600000*c.hourly,tax=gross*c.tax/100,fuel=km*c.consumption/100*c.fuelPrice,tyres=km*c.tyres,maintenance=km*c.maintenance,depreciation=km*c.depreciation,extras=s.extras||0,carCosts=fuel+tyres+maintenance+depreciation+extras;return{gross,tax,fuel,tyres,maintenance,depreciation,extras,carCosts,net:gross-tax-carCosts,workedMs:ms};}
    window.Android={state(){if(!window.testState.invoiceDraft){window.testState.invoiceDraft={profile:{...window.invoiceDefaults.profile,name:'Example Worker',client:'Example Client',bank:'Example Bank',account_name:'Example Worker',bsb:'000-000',account:'00000000',client_email:'client@example.com'},number:18,rate:'32',rows:[]};window.testState.invoices=[];window.testState.nextInvoiceNumber=18;window.testState.invoiceToday='2026-11-16';window.testState.update={stage:'idle',message:'Ready',currentVersion:'1.2.0',progress:0};}return JSON.stringify({...window.testState,totals:totals(window.testState.session)});},command(action,raw){const p=JSON.parse(raw);window.calls.push({action,p});let s=window.testState.session;
      if(action==='checkUpdate')window.testState.update={...window.testState.update,stage:'checking',message:'Checking'};
      if(action==='saveInvoiceDraft')window.testState.invoiceDraft=p;
      if(action==='issueInvoice'){
        const total=p.rows.reduce((sum,r)=>{if(r.address.toLowerCase()==='day off')return sum;const h=r.hours.includes(':')?Number(r.hours.split(':')[0])+Number(r.hours.split(':')[1])/60:Number(r.hours.replace(',','.'));return sum+Math.round(Math.ceil(h*4)/4*Number(p.rate)*100)/100;},0);
        window.testState.invoices.unshift({number:p.number,issued:'2026-11-16',client:p.profile.client,email:p.profile.client_email,total:String(total),filename:'invoice_018_2026-11-16.pdf',draft:p,sourceIds:p.rows.map(r=>r.sourceId).filter(Boolean)});
        window.testState.invoiceDraft={...p,number:p.number+1,rows:[]};window.testState.nextInvoiceNumber=p.number+1;window.invoiceIssued();
      }
      if(action==='saveConfig')window.testState.config=p;
      if(action==='commute'||(action==='work'&&!s.phase)){s=window.testState.session={phase:action==='commute'?'commute':'work',config:{...window.testState.config},startedAt:Date.now(),workedMs:0,km:0,extras:0};window.testState.serviceRunning=true;}
      else if(action==='work')s.phase='work';
      if(action==='pause')s.phase='pause';if(action==='resume')s.phase='work';if(action==='return')s.phase='return';
      if(action==='updateCosts'){s.km=p.km;s.extras=p.extras;if(p.workedMs!==undefined)s.workedMs=p.workedMs;}
      if(action==='finish'){s.phase='done';s.totals=totals(s);window.testState.history.unshift({...s});window.testState.session={};window.testState.serviceRunning=false;}
    }};
  });
  await page.goto('https://app.neonshift.local/');
  await page.locator('#status').filter({hasText:'PRONTO'}).waitFor();
  assert.equal(await page.locator('#net').innerText(),'$ 0,00');
  assert(await page.locator('#editCosts').isDisabled());
  await page.screenshot({path:'dist/tela-inicial.png',fullPage:true});
  await page.getByRole('button',{name:'↗ INICIAR TRAJETO AO TRABALHO',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='EM TRAJETO');
  await page.locator('#editCosts').click();
  await page.locator('#manualKm').fill('12,5');await page.locator('#manualExtras').fill('5,00');await page.locator('#saveCosts').click();
  await page.waitForFunction(()=>window.testState.session.km===12.5);
  await page.waitForFunction(()=>document.getElementById('net').textContent.startsWith('−'));
  assert((await page.locator('#net').innerText()).startsWith('−'));
  await page.getByRole('button',{name:'▸ CHEGUEI · INICIAR TRABALHO',exact:true}).click();
  await page.waitForFunction(()=>document.getElementById('status').textContent==='TRABALHANDO');
  await page.evaluate(()=>window.testState.session.workedMs=23400000);
  await page.waitForFunction(()=>document.getElementById('timer').textContent==='06:30:00');
  assert.equal(await page.locator('#net').innerText(),'$ 167,76');
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
  assert((await page.locator('#dailySummary').innerText()).includes('$ 167,76'));
  await page.locator('nav button[data-page="history"]').click();
  assert.equal(await page.locator('.history-card').count(),1);
  assert.equal(await page.locator('.history-net').innerText(),'$ 167,76');
  await page.locator('#history summary').click();await page.screenshot({path:'dist/tela-historico.png',fullPage:true});
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
  assert.equal(await page.locator('#net').innerText(),'$ 234,90');
  await page.evaluate(()=>window.testState.serviceRunning=false);
  await page.locator('#recovery').waitFor({state:'visible'});
  // A saved shift can be billed, with an editable work site and quarter-hour ceiling.
  await page.locator('nav button[data-page="history"]').click();
  await page.getByRole('button',{name:'＋ ADICIONAR AO INVOICE',exact:true}).click();
  assert(await page.locator('#invoices').isVisible());
  assert.equal(await page.locator('.invoice-entry').count(),1);
  await page.locator('.invoice-entry [name="workAddress"]').fill('16 East Pkwy, Fulham SA 5024');
  await page.locator('.invoice-entry [name="hours"]').fill('08:01');
  assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 264,00');
  await page.locator('#invoiceAddRow').click();
  await page.locator('.invoice-entry').nth(1).locator('[name="hours"]').fill('00:01');
  await page.locator('.invoice-entry').nth(1).locator('[name="workAddress"]').fill('Another site');
  assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 272,00');
  await page.locator('#invoiceAddRow').click();
  await page.locator('.invoice-entry').nth(2).locator('[name="workAddress"]').fill('Day off');
  await page.locator('#invoicePreview').click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='previewInvoice'&&x.p.rows.length===3)));
  assert.equal(await page.evaluate(()=>window.testState.invoices.length),0); // preview never issues
  await page.screenshot({path:'dist/tela-invoices.png',fullPage:true});
  await page.locator('#invoiceIssue').click();
  await page.waitForFunction(()=>document.querySelectorAll('#invoiceRecords article').length===1);
  assert.equal(await page.locator('.invoice-entry').count(),0); // emitted draft clears, PDF remains
  assert.equal(await page.locator('#invoiceNumber').inputValue(),'19');
  await page.getByRole('button',{name:'↗ ENVIAR AO CLIENTE',exact:true}).click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='shareInvoice'&&x.p.filename==='invoice_018_2026-11-16.pdf')));
  await page.getByRole('button',{name:'SALVAR CÓPIA',exact:true}).click();
  assert(await page.evaluate(()=>window.calls.some(x=>x.action==='saveInvoicePdf')));
  // Per-line half-cent rounding is exact, matching BigDecimal in the native engine.
  await page.locator('#invoiceAddRow').click();
  await page.locator('.invoice-entry [name="hours"]').fill('0,1');
  await page.locator('#invoiceRate').fill('0,02');
  assert.equal(await page.locator('#invoiceTotal').innerText(),'$ 0,01');
  // Narrow phone screens must not overflow horizontally.
  for(const width of [320,360,412,480])for(const height of [400,858]){await page.setViewportSize({width,height});for(const section of ['today','history','invoices','settings']){await page.locator(`nav button[data-page="${section}"]`).click();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),`overflow at ${width} on ${section}`);for(const b of await page.locator('nav button').all()){const box=await b.boundingBox();assert(box.y>=0&&box.y+box.height<=height,`nav outside viewport at ${width}x${height}`);}}}
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
  console.log('PASS: work flows, invoice draft/rounding/preview/issue/share/save dispatch, 4 pages, 4 widths, 2 heights, and navigation bounds.');
  await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
