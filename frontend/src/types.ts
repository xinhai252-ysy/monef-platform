export type Money = string | number;
export interface User {email:string; name:string;role:string;tenantId:string;demo:boolean}
export interface Account {id:string;name:string;institution:string;currency:string;type:string;openingBalance:Money;balance:Money;active:boolean}
export interface Entry {id:string;accountId:string;accountName:string;kind:'INCOME'|'EXPENSE';amount:Money;currency:string;baseAmount:Money;fxRate:Money;fxDate:string;fxSource:string;description:string;category:string;bookedOn:string;status:'POSTED'|'VOID'}
export interface Page<T>{content:T[];page:number;size:number;totalElements:number;totalPages:number}
export interface Metric {value:Money;change:Money|null}
export interface Dashboard {month:string;baseCurrency:string;income:Metric;expense:Metric;netCashFlow:Metric;surplusMargin:Money|null;transactionCount:number;series:{month:string;income:Money;expense:Money}[];categories:{category:string;amount:Money;count:number}[];generatedAt:string}
export interface Rate {base:string;quote:string;rate:Money;date:string;source:string;cached:boolean;fetchedAt:string}
export interface Conversion {base:string;quote:string;amount:Money;converted:Money;observation:Rate}
export interface ChatResult{answer:string;mode:string;month:string;currency:string;transactionCount:number;tools:string[];generatedAt:string}
export interface EntryInput{accountId:string;kind:'INCOME'|'EXPENSE';amount:string;description:string;category:string;bookedOn:string;clientRef:string}
export interface AccountInput{name:string;institution:string;currency:string;type:string;openingBalance:string}
