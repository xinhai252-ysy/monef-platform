<script setup lang="ts">
import {computed,ref} from 'vue';
import type {Dashboard} from '../types';
import {money} from '../format';
const props=defineProps<{data:Dashboard}>();
const months=ref(6);const hover=ref<number|null>(null);const mode=ref('flows');
const series=computed(()=>props.data.series.slice(-months.value));
const max=computed(()=>Math.max(100,...series.value.flatMap(p=>[Number(p.income),Number(p.expense)]))*1.2);
const x=(i:number)=>48+i*(610/(series.value.length-1));
const y=(v:number)=>210-(v/max.value)*176;
function line(kind:'income'|'expense'){return series.value.map((p,i)=>`${i?'L':'M'} ${x(i)} ${y(Number(p[kind]))}`).join(' ');}
const area=computed(()=>`${line('income')} L 658 210 L 48 210 Z`);
const selected=computed(()=>hover.value===null?null:series.value[hover.value]);
const netMax=computed(()=>Math.max(1,...series.value.map(p=>Math.abs(Number(p.income)-Number(p.expense)))));
</script>
<template>
 <section class="panel trend-panel">
  <div class="panel-heading"><div><span class="eyebrow">CASH FLOW</span><h2>资金流动趋势 <span class="unit-tag">USD</span></h2></div><select v-model="months" aria-label="图表周期" class="mini-select"><option :value="6">近 6 个月</option><option :value="12">近 12 个月</option></select></div>
  <div class="chart-tools"><div class="segments"><button :class="{active:mode==='flows'}" @click="mode='flows'">收支走势</button><button :class="{active:mode==='net'}" @click="mode='net'">净现金流</button></div><div class="legend"><span><i class="dot purple"></i>收入</span><span v-if="mode==='flows'"><i class="dot lime"></i>支出</span></div></div>
  <div class="chart-wrap" @mouseleave="hover=null">
   <svg viewBox="0 0 700 250" role="img" :aria-label="`近 ${months} 个月财务趋势，全部以美元计价`">
    <defs><linearGradient id="income-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8977ff" stop-opacity=".22"/><stop offset="1" stop-color="#8977ff" stop-opacity="0"/></linearGradient><linearGradient id="bar-fill" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#a397ff"/><stop offset="1" stop-color="#5745d6"/></linearGradient></defs>
    <template v-if="mode==='flows'">
     <g v-for="tick in [0,1,2,3,4]" :key="tick"><line x1="48" x2="670" :y1="34+tick*44" :y2="34+tick*44" stroke="#ffffff0b" stroke-dasharray="3 5"/><text x="0" :y="38+tick*44" class="axis">{{Math.round(max*(4-tick)/4/1000)}}k</text></g>
     <path :d="area" fill="url(#income-fill)"/><path :d="line('income')" fill="none" stroke="#9a87ff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
     <path :d="line('expense')" fill="none" stroke="#a2f3ad" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
     <g v-for="(point,i) in series" :key="point.month"><circle :cx="x(i)" :cy="y(Number(point.income))" r="4" fill="#9a87ff" stroke="#232228" stroke-width="2"/><rect :x="x(i)-24" y="20" width="48" height="192" fill="transparent" @mouseenter="hover=i"/></g>
     <line v-if="hover!==null" :x1="x(hover)" :x2="x(hover)" y1="28" y2="210" stroke="#b1a1ff60" stroke-dasharray="4"/>
    </template>
    <template v-else><line x1="48" x2="670" y1="122" y2="122" stroke="#ffffff22"/><text x="12" y="126" class="axis">0</text><g v-for="(p,i) in series" :key="p.month"><rect :x="x(i)-15" :y="Number(p.income)>=Number(p.expense)?122-(Number(p.income)-Number(p.expense))/netMax*85:122" width="30" :height="Math.max(1,Math.abs(Number(p.income)-Number(p.expense))/netMax*85)" rx="5" :fill="Number(p.income)>=Number(p.expense)?'url(#bar-fill)':'#e58c91'" @mouseenter="hover=i"/></g></template>
    <text v-for="(p,i) in series" :key="p.month" :x="x(i)" y="239" text-anchor="middle" class="axis">{{Number(p.month.slice(-2))}}月</text>
   </svg>
   <div v-if="selected" class="chart-tooltip" :style="{left:Math.min(72,Math.max(8,(hover??0)/(series.length-1)*80))+'%'}"><strong>{{selected.month}}</strong><span>收入 {{money(selected.income)}}</span><span>支出 {{money(selected.expense)}}</span></div>
  </div>
  <div class="chart-footer"><span><span class="live-dot"></span>基于已入账交易</span><span>作废流水不计入统计</span></div>
 </section>
</template>
