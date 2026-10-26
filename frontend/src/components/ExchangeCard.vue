<script setup lang="ts">
import {ref,watch,onMounted} from 'vue';
import {ArrowDownUp,ArrowUpRight,RefreshCw} from 'lucide-vue-next';
import {api} from '../api';
import {money,today} from '../format';
import type {Conversion,Rate,Page} from '../types';
defineProps<{expanded?:boolean}>();
const base=ref('USD'),quote=ref('EUR'),amount=ref('1200.00'),date=ref(''),result=ref<Conversion|null>(null),loading=ref(false),error=ref('');
const history=ref<Page<Rate>|null>(null),historyPage=ref(0);let sequence=0;
const currencies=['USD','EUR','GBP','CNY','JPY','SGD','HKD','AUD','CAD','CHF'];
const flags:Record<string,string>={USD:'US',EUR:'EU',GBP:'GB',CNY:'CN',JPY:'JP',SGD:'SG',HKD:'HK',AUD:'AU',CAD:'CA',CHF:'CH'};
async function loadHistory(){try{history.value=await api.history(base.value,quote.value,historyPage.value);}catch(e){error.value=(e as Error).message;}}
async function convert(){const id=++sequence;error.value='';loading.value=true;try{const value=await api.convert(base.value,quote.value,amount.value,date.value||undefined);if(id===sequence){result.value=value;await loadHistory();}}catch(e){if(id===sequence){error.value=(e as Error).message;result.value=null;}}finally{if(id===sequence)loading.value=false;}}
function swap(){[base.value,quote.value]=[quote.value,base.value];}
watch([base,quote,date],()=>{result.value=null;historyPage.value=0;void convert();});
watch(amount,()=>{sequence++;result.value=null;loading.value=false;});onMounted(convert);
</script>
<template>
 <section class="panel exchange-card" :class="{expanded}">
  <div class="panel-heading"><div><span class="eyebrow">GLOBAL MONEY</span><h2>多币种换算</h2></div><span class="icon-disc"><ArrowDownUp :size="17"/></span></div>
  <div v-if="expanded" class="fx-date"><label for="fx-date">汇率日期</label><input id="fx-date" type="date" v-model="date" min="2000-01-01" :max="today()"/><button v-if="date" class="text-button" @click="date=''">使用最新汇率</button></div>
  <div class="exchange-field"><label>换算金额</label><div class="currency-row"><span class="flag" :class="base.toLowerCase()">{{flags[base]}}</span><select v-model="base" aria-label="原始币种"><option v-for="c in currencies" :key="c">{{c}}</option></select><input v-model="amount" type="number" min="0" max="9999999999999.99" step="0.01" aria-label="换算金额" @keydown.enter="convert"/></div></div>
  <div class="swap-line"><button @click="swap" aria-label="交换币种"><ArrowDownUp :size="16"/></button></div>
  <div class="exchange-field output"><label>预计可兑换</label><div class="currency-row"><span class="flag" :class="quote.toLowerCase()">{{flags[quote]}}</span><select v-model="quote" aria-label="目标币种"><option v-for="c in currencies" :key="c">{{c}}</option></select><strong>{{result?money(result.converted,quote,2):'—'}}</strong></div></div>
  <button class="exchange-button" :disabled="loading" @click="convert">{{loading?'正在查询…':'计算兑换金额'}}<RefreshCw v-if="loading" :size="15" class="spin"/><ArrowUpRight v-else :size="16"/></button>
  <p v-if="error" class="inline-error" role="alert">{{error}}</p>
  <div class="rate-caption" v-if="result"><span>1 {{base}} = {{Number(result.observation.rate).toFixed(4)}} {{quote}}</span><span>{{result.observation.cached?'缓存命中':'已更新'}}</span></div>
  <p class="source-note">{{result?.observation.source==='DEMO'?'演示固定汇率':result?.observation.source==='IDENTITY'?'同币种换算':'Frankfurter 日参考汇率'}}<span v-if="result"> · {{result.observation.date}}</span> · 仅计算，不执行兑换</p>
 </section>
 <section v-if="expanded" class="panel fx-history"><div class="panel-heading"><div><span class="eyebrow">RATE OBSERVATIONS</span><h2>汇率历史记录</h2></div><span class="unit-tag">{{base}} / {{quote}}</span></div><div class="table-scroll"><table><thead><tr><th>汇率日期</th><th>汇率</th><th>数据来源</th><th>查询时间</th></tr></thead><tbody><tr v-for="(r,i) in history?.content" :key="i"><td>{{r.date}}</td><td class="numeric">{{Number(r.rate).toFixed(6)}}</td><td>{{r.source==='DEMO'?'演示固定值':r.source}}</td><td>{{new Date(r.fetchedAt).toLocaleString('zh-CN')}}</td></tr></tbody></table></div><p v-if="!history?.content.length" class="empty">完成换算后，查询记录会出现在这里。</p><div class="pagination" v-if="history&&history.totalPages>1"><button :disabled="historyPage===0" @click="historyPage--;loadHistory()">上一页</button><span>{{historyPage+1}} / {{history.totalPages}}</span><button :disabled="historyPage+1>=history.totalPages" @click="historyPage++;loadHistory()">下一页</button></div></section>
</template>
