const assert=require('node:assert/strict'),{parse,prompt}=require('../app/src/main/assets/receipt-ai.js');
const source={schemaVersion:1,type:'menatwork.receipt',merchant:'Test Hardware',date:'2026-11-26',currency:'AUD',category:'materials',reference:'123',items:[{name:'Screws',description:'Box 50',quantity:'2',unit:'ea',unitPrice:'5.25',amount:'10.50'}],total:'10.50',tax:'0.95',litres:null,unitPrice:null,notes:null,rawText:'Screws 10.50',warnings:[]};
assert.match(prompt,/Never invent/);assert.equal(parse(JSON.stringify(source)).items[0].quantity,'2');assert.equal(parse('```json\n'+JSON.stringify(source)+'\n```').total,'10.50');
for(const changes of [{schemaVersion:2},{items:{}},{date:'2026-02-30'},{total:'$10.50'},{total:'1e999'},{items:[{name:'A',amount:'1.999'}]},{currency:'GBP'},{warnings:'ok'},{category:'unknown'}])assert.throws(()=>parse(JSON.stringify({...source,...changes})));
assert.throws(()=>parse('hello '+JSON.stringify(source)));assert.throws(()=>parse('[]'));
const missing=parse(JSON.stringify({...source,date:null,currency:null,total:null}));assert.equal(missing.date,'');assert.equal(missing.total,'');
const safe=parse(JSON.stringify({...source,id:'overwrite',file:'secret.img',reviewed:true,workPercent:'0',client:'Injected'}));for(const key of ['id','file','reviewed','workPercent','client'])assert.equal(key in safe,false);
const fuel=parse(JSON.stringify({...source,category:'fuel',litres:'30.123',unitPrice:'1.899',items:[{name:'Fuel',amount:'57.20'},{name:'Discount',amount:'-0.10'}]}));assert.equal(fuel.items[1].amount,'-0.10');assert.equal(fuel.unitPrice,'1.899');
console.log('PASS: AI receipt JSON contract, missing values, fences, validation, fuel precision, discounts and untrusted metadata isolation.');
