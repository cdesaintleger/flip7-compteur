import assert from 'node:assert/strict';
import {details,roundScores,freshHand} from '../dist/scoring.mjs';
const h=(cards=[],extra={})=>({...freshHand(),cards,...extra});
assert.equal(details(h()).score,0);
assert.equal(details(h(['10','11','minus4','half'])).score,8); // (21−4)/2 arrondi inférieur
assert.equal(details(h(['1','minus10','half'])).score,0);
assert.equal(details(h(['1','minus10','half']),'brutal').score,-5);
assert.equal(details(h(['0','13'])).score,0);
assert.equal(details(h(['0','1','2','3','4','5','6'])).score,36);
assert.equal(details(h(['1','2','3','4','5','6','7','half','minus4'])).score,27);
assert.equal(details(h(['13','lucky13'])).score,26);
assert.equal(details(h(['13','lucky13','1','2','3','4','5'])).score,56);
assert.equal(details(h(['unlucky7','8'])).score,15);
assert.equal(details(h(['13','minus10'],{bust:true})).score,0);
assert.equal(details(h(['13','minus10'],{bust:true}),'brutal').score,-10);
assert.deepEqual(roundScores([h(['1','2','3','4','5','6','7'],{target:1}),h()],'brutal'),[28,-15]);
assert.equal(details(h([],{manual:49})).score,49);
console.log('PASS: FR scoring order, rounding, minimum, zero, automatic bonus, lucky 13, unlucky 7, bust, brutal mode, vengeance and old scores');
