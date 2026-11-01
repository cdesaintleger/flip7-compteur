import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import {fileURLToPath} from 'node:url';
import {numberCards, specials, penalties} from '../dist/scoring.mjs';

const dist=fileURLToPath(new URL('../dist/',import.meta.url));
const source=fs.readFileSync(path.join(dist,'sw.js'),'utf8');
const context=vm.createContext({self:{addEventListener(){}}});
vm.runInContext(source,context);
const files=Array.from(vm.runInContext('FILES',context));
assert.equal(new Set(files).size,files.length,'Le précache ne doit pas contenir de doublons.');
for(const file of files){
  assert(file.startsWith('./'),'Les chemins doivent fonctionner dans un sous-dossier GitHub Pages.');
  const resolved=path.resolve(dist,file);
  assert(resolved===path.resolve(dist)||resolved.startsWith(dist),'Le fichier doit rester dans dist/.');
  assert(fs.existsSync(resolved),'Fichier hors connexion manquant : '+file);
}
const required=['./','./index.html','./app.js','./scoring.mjs','./style.css','./theme.css','./manifest.webmanifest'];
for(const card of [...numberCards,...specials,...penalties,{id:'half'}]){
  const file='./assets/cards/'+card.id+'.png';required.push(file);
  const image=fs.readFileSync(path.join(dist,file));
  assert.equal(image.subarray(1,4).toString(),'PNG','Carte PNG invalide : '+card.id);
  assert(image.readUInt32BE(16)>200&&image.readUInt32BE(20)>300,'Résolution insuffisante : '+card.id);
}
const manifest=JSON.parse(fs.readFileSync(path.join(dist,'manifest.webmanifest'),'utf8'));
assert.equal(manifest.start_url,'./');assert.equal(manifest.scope,'./');assert.equal(manifest.display,'standalone');
for(const icon of manifest.icons)required.push('./'+icon.src);
for(const file of required)assert(files.includes(file),'Ressource absente du précache : '+file);
console.log('PASS: précache complet, 22 cartes individuelles, icônes et chemins relatifs compatibles GitHub Pages');
