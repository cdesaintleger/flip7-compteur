export const numberCards=Array.from({length:14},(_,n)=>({id:String(n),value:n,label:String(n)}));
export const specials=[{id:'unlucky7',value:7,label:'7',name:'7 porte-malheur'},{id:'lucky13',value:13,label:'13',name:'13 porte-bonheur'}];
export const penalties=[2,4,6,8,10].map(n=>({id:'minus'+n,label:'−'+n,value:-n}));
export const freshHand=()=>({cards:[],bust:false,target:null,manual:null});
export function details(hand,mode='standard'){
 if(Number.isInteger(hand.manual))return {score:hand.manual,count:0,sum:0,penalty:0,bonus:0,base:hand.manual,legacy:true};
 const cards=new Set(hand.cards),numbers=[...numberCards,...specials].filter(c=>cards.has(c.id));
 const count=numbers.length,sum=numbers.reduce((a,c)=>a+c.value,0),penalty=penalties.filter(c=>cards.has(c.id)).reduce((a,c)=>a-c.value,0);
 const flip=count===7&&!hand.bust,bonus=flip&&hand.target===null?15:0;
 let base=hand.bust||cards.has('0')&&!flip?0:sum;
 // Livret français p.6 : soustraction, minimum 0 en standard, division, bonus.
 if(!hand.bust||mode==='brutal')base-=penalty;
 if(mode!=='brutal')base=Math.max(0,base);
 if(cards.has('half'))base=Math.floor(base/2);
 return {score:base+bonus,count,sum,penalty,bonus,base,flip,zero:cards.has('0')&&!flip,bust:hand.bust,half:cards.has('half')};
}
export function roundScores(hands,mode='standard'){
 const result=hands.map(h=>details(h,mode).score);
 if(mode==='brutal')hands.forEach((h,i)=>{if(details(h,mode).flip&&Number.isInteger(h.target)&&h.target!==i&&h.target>=0&&h.target<hands.length)result[h.target]-=15});
 return result;
}
