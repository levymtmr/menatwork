'use strict';
(() => {
 const themes={pipboy:{name:'Pip-Boy',art:'Trabalhador em um terminal de fósforo verde'},medieval:{name:'Medieval pixel art',art:'Ferreiro em pixel art trabalhando diante de uma fortaleza'},cyberpunk:{name:'Cyberpunk',art:'Operador em pixel art numa oficina de uma cidade neon'}};
 let selected='pipboy';
 try{selected=typeof Android!=='undefined'?JSON.parse(Android.state()).theme:localStorage.getItem('menatwork.theme');}catch(e){}
 function apply(value){selected=themes[value]?value:'pipboy';document.documentElement.dataset.theme=selected;document.querySelector('meta[name="theme-color"]').content={pipboy:'#08120a',medieval:'#18130f',cyberpunk:'#090e20'}[selected];document.querySelectorAll('[data-theme-choice]').forEach(b=>{const active=b.dataset.themeChoice===selected;b.setAttribute('aria-pressed',String(active));b.classList.toggle('selected',active);});document.getElementById('city').setAttribute('aria-label',themes[selected].art);window.dispatchEvent(new CustomEvent('themechange',{detail:selected}));}
 document.querySelectorAll('[data-theme-choice]').forEach(b=>b.onclick=()=>{const value=b.dataset.themeChoice;apply(value);if(typeof Android!=='undefined')Android.command('saveTheme',JSON.stringify({theme:value}));else try{localStorage.setItem('menatwork.theme',value);}catch(e){}});
 window.ThemeUI={render(value){if(themes[value]&&value!==selected)apply(value);},current:()=>selected};apply(selected);
})();
