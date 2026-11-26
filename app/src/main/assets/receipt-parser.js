'use strict';
(() => {
 const money=raw=>{let s=String(raw).replace(/[^\d.,-]/g,'');const dot=s.lastIndexOf('.'),comma=s.lastIndexOf(',');if(comma>dot)s=s.replace(/\./g,'').replace(',','.');else s=s.replace(/,/g,'');const n=Number(s);return Number.isFinite(n)?n.toFixed(2):'';};
 function parse(text){
  const lines=String(text||'').split(/\r?\n/).map(s=>s.trim()).filter(Boolean),out={merchant:'',date:'',currency:'',category:'materials',total:'',tax:'',litres:'',unitPrice:'',items:[]};
  const date=String(text).match(/\b(20\d{2})[-/.](\d{1,2})[-/.](\d{1,2})\b/)||String(text).match(/\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2}|20\d{2})\b/);
  if(date){const iso=date[1].length===4?[date[1],date[2],date[3]]:[date[3].length===2?'20'+date[3]:date[3],date[2],date[1]],value=iso.map((s,i)=>i?s.padStart(2,'0'):s).join('-'),d=new Date(value+'T12:00:00Z');if(Number.isFinite(d.getTime())&&d.toISOString().slice(0,10)===value)out.date=value;}
  const currency=String(text).match(/\b(AUD|USD|NZD|CAD|BRL|EUR)\b/i);if(currency)out.currency=currency[1].toUpperCase();else if(/R\$/.test(text))out.currency='BRL';
  const administrative=/\b(?:subtotal|sub total|total|amount due|valor total|a pagar|gst|iva|tax|change|troco|cash|dinheiro|eftpos|visa|mastercard|debit|credit|card|approved|auth|balance|terminal|payment|payment received|abn|cnpj|cpf|telefone|phone|tel|thank|obrigad|receipt|tax invoice|nota fiscal|coupon|loyalty|reward|points|invoice number)\b/i;
  out.merchant=lines.slice(0,7).find(line=>/[A-Za-zÀ-ÿ]{3}/.test(line)&&!administrative.test(line)&&!/^\d/.test(line)&&!/(?:https?:|www\.|@)/i.test(line))||'';
  let totalRank=0,pending='';
  for(const line of lines){
   const value=line.match(/(?:R\$|\$|€|£)?\s*(-?(?:\d{1,3}(?:[., ]\d{3})+|\d+)[.,]\d{2})(?:\s*(?:AUD|USD|BRL|EUR|NZD|CAD|[A-Z]))?\s*$/i);
   if(!value){if(/[A-Za-zÀ-ÿ]{3}/.test(line)&&!administrative.test(line)&&line!==out.merchant)pending=line;continue;}
   const amount=money(value[1]);if(amount==='')continue;
   const mainTotal=/\b(?:grand total|total due|amount due|valor total|total a pagar)\b|^total\b(?!\s*(?:gst|iva|tax)\b)/i.test(line);
   if(/\b(?:gst|iva|tax|imposto)\b/i.test(line)&&!mainTotal){out.tax=amount;continue;}
   let rank=mainTotal?3:/\b(?:subtotal|sub total)\b/i.test(line)?1:0;
   if(/\b(?:discount|saving|desconto)\b/i.test(line))rank=0;
   if(rank){if(rank>=totalRank){out.total=amount;totalRank=rank;}continue;}
   if(administrative.test(line))continue;
   let name=line.slice(0,value.index).trim().replace(/[.$€£:]+$/,'').trim();if(!/[A-Za-zÀ-ÿ]{2}/.test(name)&&pending)name=pending;if(!/[A-Za-zÀ-ÿ]{2}/.test(name))continue;
   const description=name;name=name.replace(/^\d{4,}\s+/,'').replace(/\s+\d+(?:[.,]\d+)?\s*(?:x|@).*$/i,'').trim();out.items.push({name:name.slice(0,200),description:description===name?'':description.slice(0,500),amount});pending='';
  }
  if(/\b(?:unleaded|diesel|petrol|gasolina|combust[ií]vel|ethanol|e10|fuel)\b/i.test(text)){
   out.category='fuel';const litres=String(text).match(/(\d+(?:[.,]\d{1,3})?)\s*(?:litres?|litros?|l)\b/i),price=String(text).match(/(?:@|price\/l|pre[cç]o\/l)\s*\$?\s*(\d+(?:[.,]\d{1,4})?)/i);if(litres)out.litres=litres[1].replace(',','.');if(price)out.unitPrice=price[1].replace(',','.');
  }
  out.items=out.items.slice(0,200);return out;
 }
 if(typeof module!=='undefined')module.exports={parse,money};else window.ReceiptParser={parse,money};
})();
