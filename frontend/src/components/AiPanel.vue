<script setup lang="ts">
import {ref,watch,nextTick} from 'vue';
import {ArrowUp,Sparkles,ShieldCheck,Check,ArrowUpRight} from 'lucide-vue-next';
import {api,previewMode} from '../api';
import type {ChatResult} from '../types';
const props=defineProps<{month:string;expanded?:boolean}>();
const emit=defineEmits<{open:[]}>();
const question=ref(''),busy=ref(false),error=ref(''),messages=ref<{role:string;text:string;data?:ChatResult}[]>([]);const log=ref<HTMLElement|null>(null);
const prompts=['分析本月的收支表现','哪些支出值得关注？','本月现金流健康吗？'];
watch(()=>props.month,()=>{messages.value=[];error.value='';});
async function send(text=question.value){if(busy.value||!text.trim())return;const month=props.month;question.value='';error.value='';messages.value.push({role:'user',text});busy.value=true;try{const data=await api.chat(text,month);if(month===props.month)messages.value.push({role:'assistant',text:data.answer,data});}catch(e){error.value=(e as Error).message;}finally{busy.value=false;await nextTick();log.value?.scrollTo({top:log.value.scrollHeight,behavior:'smooth'});}}
</script>
<template>
 <section class="panel ai-card" :class="{expanded}">
  <div class="panel-heading"><div class="ai-title"><span class="ai-orb"><Sparkles :size="19"/></span><div><h2>Monef AI <span class="beta">ASSISTANT</span></h2><span class="ai-subtitle">把财务数据，变成清晰洞察</span></div></div><button v-if="!expanded" class="icon-button" aria-label="打开 AI 助手" @click="emit('open')"><ArrowUpRight :size="19"/></button></div>
  <template v-if="!expanded"><p class="ai-teaser">这个月的业务表现如何？<br/>让数据给你答案。</p><button class="ai-prompt" @click="emit('open')"><Sparkles :size="15"/>分析本月财务状况 <ArrowUpRight :size="15"/></button><div class="ai-note"><ShieldCheck :size="13"/>只读访问 · 基于账本数据</div></template>
  <template v-else>
   <div class="ai-context"><span><span class="live-dot"></span>{{month}} · USD 分析口径</span><span>{{previewMode?'交互演示 · 固定数据摘要':'只读查询 · 最多 6 次工具调用'}}</span></div>
   <div class="chat-log" ref="log" aria-live="polite">
    <div v-if="!messages.length" class="chat-welcome"><div class="large-orb"><Sparkles :size="36"/></div><h3>你的财务数据，值得被读懂。</h3><p>从收支变化到支出结构，基于当前账本生成有依据的分析。</p><div class="prompt-list"><button v-for="p in prompts" :key="p" @click="send(p)">{{p}}<ArrowUpRight :size="16"/></button></div></div>
    <div v-for="(m,i) in messages" :key="i" class="chat-message" :class="m.role"><div class="message-label">{{m.role==='user'?'你':'Monef · '+(m.data?.mode==='DATA_SUMMARY'?'数据摘要':'AI 分析')}}</div><p>{{m.text}}</p><div v-if="m.data" class="evidence"><Check :size="13"/>依据 {{m.data.month}} 的 {{m.data.transactionCount}} 笔有效交易 · {{m.data.tools.map(t=>t==='monthlySummary'?'月度汇总':'交易明细').join(' / ')}}</div></div>
    <div v-if="busy" class="thinking"><i></i><i></i><i></i><span>正在查询财务数据…</span></div>
   </div>
   <p v-if="error" class="inline-error" role="alert">{{error}}</p>
   <form class="chat-compose" @submit.prevent="send()"><textarea v-model="question" placeholder="问问 Monef，关于你的财务…" maxlength="1000" aria-label="财务问题" rows="2" @keydown.enter.exact.prevent="send()"></textarea><button :disabled="busy||!question.trim()" aria-label="发送问题"><ArrowUp :size="20"/></button></form>
   <p class="source-note"><ShieldCheck :size="12"/> 仅分析已入账数据，不执行转账；未配置模型时明确返回数据摘要。</p>
  </template>
 </section>
</template>
