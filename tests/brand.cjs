const {chromium}=require('playwright');
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:512,height:512},deviceScaleFactor:1});
 const svg=fs.readFileSync(path.join(__dirname,'../design/brand/menatwork-icon.svg'),'utf8');
 await page.setContent('<style>html,body{margin:0;width:512px;height:512px;background:transparent}svg{display:block;width:512px;height:512px}</style>'+svg);
 await page.screenshot({path:'design/brand/menatwork-icon.png',omitBackground:true});
 await page.setViewportSize({width:760,height:430});await page.setContent('<style>body{margin:0;padding:32px;background:#e7ddbf;font:16px Arial;color:#211914}h1{margin:0 0 24px}.examples{display:flex;gap:32px;align-items:center}.icon{width:160px;height:160px;overflow:hidden}.circle{border-radius:50%}.squircle{border-radius:28%}.small{width:48px;height:48px}.icon svg{width:100%;height:100%}p{margin-top:24px}</style><h1>MENATWORK</h1><div class="examples"><div class="icon">'+svg+'</div><div class="icon circle">'+svg+'</div><div class="icon squircle">'+svg+'</div><div class="icon small">'+svg+'</div></div><p>Adaptive icon · circular mask · Samsung shape · 48 px</p>');
 await page.screenshot({path:'dist/brand-icon-masks.png'});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.setViewportSize({width:412,height:858});await page.goto('file://'+path.join(__dirname,'../app/src/main/assets/index.html'));
 for(const theme of ['retro','pipboy','medieval','cyberpunk']){await page.locator('nav [data-page="settings"]').click();await page.locator(`[data-theme-choice="${theme}"]`).click();assert.equal(await page.locator('header .menatwork-mark path').count(),3);assert.equal(await page.locator('header .menatwork-mark').evaluate(e=>getComputedStyle(e).stroke),'none');assert(await page.locator('header .brand-icon').isVisible());assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:`dist/brand-header-${theme}.png`});}
 assert.deepEqual(errors,[]);await browser.close();console.log('PASS: logo rendered at 512px, Android mask previews, four theme headers and phone-width layout.');
})().catch(e=>{console.error(e);process.exit(1)});
