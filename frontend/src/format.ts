import Decimal from 'decimal.js';
import type { Money } from './types';
export function money(value:Money, currency='USD', digits=0) {
 const amount=new Decimal(value).toFixed(digits);const [integer,decimal]=amount.split('.');
 const symbols:Record<string,string>={USD:'$',EUR:'€',GBP:'£',CNY:'¥',JPY:'¥',SGD:'S$',HKD:'HK$',AUD:'A$',CAD:'C$',CHF:'CHF '};
 const sign=integer.startsWith('-')?'-':'';return sign+(symbols[currency]??currency+' ')+integer.replace('-','').replace(/\B(?=(\d{3})+(?!\d))/g,',')+(decimal?'.'+decimal:'');
}
export function percent(value:Money|null){if(value===null)return '—';const n=new Decimal(value);return `${n.greaterThan(0)?'+':''}${n.toFixed(1)}%`;}
export const currentMonth=()=>new Date().toISOString().slice(0,7);
export const today=()=>new Date().toISOString().slice(0,10);
export function monthBounds(month:string){const [year,m]=month.split('-').map(Number);return {start:`${month}-01`,end:`${month}-${new Date(Date.UTC(year,m,0)).getUTCDate()}`};}
export function csvCell(value:unknown){let s=String(value??'');if(/^[=+\-@\t\r]/.test(s))s="'"+s;return '"'+s.replaceAll('"','""')+'"';}
