import {useEffect,useMemo,useState} from 'react';
import {Alert,Avatar,Box,Button,Card,CardContent,Checkbox,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Divider,FormControlLabel,LinearProgress,MenuItem,Radio,RadioGroup,Select,Stack,Tab,Tabs,TextField,Typography} from '@mui/material';
import {AssignmentRounded,EventAvailableRounded,NotificationsRounded,PaymentsRounded,QuizRounded,SchoolRounded,CalendarMonthRounded,ArrowForwardRounded,CheckCircleRounded,ScheduleRounded,AccessTimeRounded} from '@mui/icons-material';
import {api} from '../services';
import {useAuth} from './auth/authStore';

function unwrap(r:any){return r?.data?.data??r?.data??{}}
const money=(n:any)=>'₹'+Number(n||0).toLocaleString('en-IN',{maximumFractionDigits:2});
const date=(v:any)=>v?new Date(v).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'}):'—';
const dateTime=(v:any)=>v?new Date(v).toLocaleString('en-IN',{day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}):'—';
const dayName=(n:any)=>['','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'][Number(n)]||'—';

export default function PortalPage(){
 const session=useAuth(s=>s.session);
 const [children,setChildren]=useState<any[]>([]),[id,setId]=useState(''),[data,setData]=useState<any>(),[error,setError]=useState(''),[loading,setLoading]=useState(true),[tab,setTab]=useState(0),[exam,setExam]=useState<any>(null);
 const isStudent=session?.roles?.includes('STUDENT');
 const load=async()=>{
  setLoading(true);setError('');
  try{const d=unwrap(await api.get('/portal/me'));setChildren(d.children||[]);if(!id&&d.children?.[0]?.id)setId(d.children[0].id);}
  catch(e:any){setError(e?.response?.data?.message||'Unable to load portal')}finally{setLoading(false)}
 };
 const loadStudent=async(sid=id)=>{
  if(!sid)return;
  try{setError('');setData(unwrap(await api.get('/portal/student/'+sid)));}
  catch(e:any){setError(e?.response?.data?.message||'Unable to load student portal')}
 };
 useEffect(()=>{load()},[]);
 useEffect(()=>{loadStudent()},[id]);
 if(loading&&!children.length)return <Card><CardContent sx={{py:10,textAlign:'center'}}><LinearProgress sx={{mb:3}}/><Typography sx={{color:'#64748b'}}>Loading your learning workspace…</Typography></CardContent></Card>;
 if(error&&!data)return <Alert severity="error" sx={{borderRadius:3}}>{error}</Alert>;
 if(!children.length)return <Empty title="No linked student profile" text="Your institute has not linked a student to this portal account yet."/>;

 return <Stack spacing={2.25}>
  <Hero data={data} childrenRows={children} id={id} setId={setId} isStudent={isStudent}/>
  {error&&<Alert severity="warning" onClose={()=>setError('')}>{error}</Alert>}
  <Tabs value={tab} onChange={(_,v)=>setTab(v)} variant="scrollable" allowScrollButtonsMobile sx={{bgcolor:'#fff',border:'1px solid #e8edf3',borderRadius:2.5,px:1}}>
   {['Overview','Attendance','Homework','Results','Fees','Schedule','Exams','Notifications'].map(x=><Tab key={x} label={x} sx={{fontWeight:800,fontSize:12}}/>)}
  </Tabs>
  {!data?<Card><CardContent sx={{py:8,textAlign:'center'}}><Typography>Loading student data…</Typography></CardContent></Card>:
   <>
    {tab===0&&<Overview data={data} setTab={setTab}/>}
    {tab===1&&<Attendance data={data}/>}
    {tab===2&&<Homework data={data} isStudent={isStudent} onRefresh={()=>loadStudent(id)}/>}
    {tab===3&&<Results data={data}/>}
    {tab===4&&<Fees data={data}/>}
    {tab===5&&<Schedule data={data}/>}
    {tab===6&&<Exams data={data} studentId={id} onOpen={setExam}/>}
    {tab===7&&<Notifications data={data}/>}
   </>
  }
  {exam&&<ExamDialog exam={exam} studentId={id} onClose={()=>setExam(null)} onSubmitted={()=>{setExam(null);loadStudent(id)}}/>}
 </Stack>
}

function Hero({data,childrenRows,id,setId,isStudent}:{data:any;childrenRows:any[];id:string;setId:(x:string)=>void;isStudent?:boolean}){
 const s=data?.student;
 return <Card sx={{overflow:'hidden',background:'linear-gradient(135deg,#0f766e 0%,#115e59 100%)',color:'#fff',border:0}}>
  <CardContent sx={{p:{xs:2.25,md:3}}}>
   <Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}>
    <Avatar sx={{width:58,height:58,bgcolor:'rgba(255,255,255,.16)',color:'#fff',fontSize:20,fontWeight:900}}>{(s?.first_name||'S')[0]}</Avatar>
    <Box sx={{flex:1}}>
     <Typography sx={{fontSize:11,textTransform:'uppercase',letterSpacing:1.2,opacity:.72,fontWeight:800}}>My Learning</Typography>
     <Typography sx={{fontSize:{xs:23,md:28},fontWeight:950,letterSpacing:-.8}}>{s?.first_name} {s?.last_name||''}</Typography>
     <Typography sx={{fontSize:12.5,opacity:.82,mt:.25}}>{s?.admission_number} · {data?.enrollment?.class_name||'Class not assigned'} · {data?.enrollment?.batch_name||'Batch not assigned'}</Typography>
    </Box>
    {!isStudent&&childrenRows.length>1&&<Select size="small" value={id} onChange={e=>setId(e.target.value)} sx={{minWidth:190,color:'#fff',borderRadius:2,'& .MuiOutlinedInput-notchedOutline':{borderColor:'rgba(255,255,255,.4)'},'& .MuiSvgIcon-root':{color:'#fff'}}}>{childrenRows.map(c=><MenuItem key={c.id} value={c.id}>{c.first_name} {c.last_name||''}</MenuItem>)}</Select>}
   </Stack>
  </CardContent>
 </Card>
}

function Overview({data,setTab}:{data:any;setTab:(n:number)=>void}){
 const att=(data.attendance||[]).reduce((a:any,x:any)=>a+Number(x.total||0),0), present=(data.attendance||[]).find((x:any)=>String(x.status).toUpperCase()==='PRESENT')?.total||0;
 const pct=att?Math.round(Number(present)/att*100):0;
 const due=(data.fees||[]).reduce((a:any,x:any)=>a+Math.max(Number(x.balance??(Number(x.amount||0)-Number(x.paid_amount||0))),0),0);
 const pending=(data.homework||[]).filter((x:any)=>!['SUBMITTED','GRADED'].includes(String(x.submission_status).toUpperCase())).length;
 return <Stack spacing={2.25}>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',md:'repeat(4,1fr)'},gap:1.5}}>
   <Kpi icon={EventAvailableRounded} label="Attendance" value={pct+'%'} sub={att+' records'}/>
   <Kpi icon={AssignmentRounded} label="Homework" value={pending} sub="pending"/>
   <Kpi icon={QuizRounded} label="Results" value={(data.results||[]).length} sub="published"/>
   <Kpi icon={PaymentsRounded} label="Fees due" value={money(due)} sub="outstanding"/>
  </Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'1.25fr .75fr'},gap:1.5}}>
   <Card><CardContent><SectionTitle title="Continue learning" text="Jump directly to what needs your attention."/>
    <Stack spacing={1}>
     <ActionRow icon={AssignmentRounded} title={pending?pending+' homework item'+(pending>1?'s':'')+' pending':'Homework is up to date'} text="View assignments and submission status" onClick={()=>setTab(2)} badge={pending?String(pending):'✓'}/>
     <ActionRow icon={EventAvailableRounded} title={pct+'% attendance'} text="Review your recent attendance history" onClick={()=>setTab(1)} badge={pct+'%'}/>
     <ActionRow icon={QuizRounded} title={(data.exams||[]).length+' upcoming exam'+((data.exams||[]).length!==1?'s':'')} text="See exam schedule and available attempts" onClick={()=>setTab(6)} badge="View"/>
    </Stack>
   </CardContent></Card>
   <Card><CardContent><SectionTitle title="Academic profile" text="Your current enrollment"/>
    <Stack spacing={1.25}>{[['Academic year',data.enrollment?.academic_year_name],['Class',data.enrollment?.class_name],['Stream',data.enrollment?.stream_name||'—'],['Batch',data.enrollment?.batch_name]].map(([k,v])=><Row key={k as string} label={k as string} value={v||'—'}/>)}</Stack>
   </CardContent></Card>
  </Box>
 </Stack>
}

function Attendance({data}:{data:any}){
 const total=(data.attendance||[]).reduce((a:any,x:any)=>a+Number(x.total||0),0),present=(data.attendance||[]).find((x:any)=>String(x.status).toUpperCase()==='PRESENT')?.total||0,pct=total?Math.round(Number(present)/total*100):0;
 return <Stack spacing={2}>
  <Card><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Box sx={{flex:1}}><SectionTitle title="Attendance overview" text="Your attendance across recorded sessions"/><LinearProgress variant="determinate" value={pct} sx={{height:9,borderRadius:8,mt:2}}/></Box><Typography sx={{fontSize:38,fontWeight:950,color:'#0f766e'}}>{pct}%</Typography></Stack></CardContent></Card>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',sm:'repeat(4,1fr)'},gap:1.5}}>{(data.attendance||[]).map((x:any)=><Kpi key={x.status} label={String(x.status).replaceAll('_',' ')} value={x.total} sub="sessions"/>)}</Box>
  <Card><CardContent><SectionTitle title="Recent attendance" text="Latest 30 recorded sessions"/>
   <Stack divider={<Divider/>}>{(data.attendanceRecent||[]).map((x:any,i:number)=><Stack key={i} direction="row" spacing={1.5} sx={{py:1.35,alignItems:'center'}}><Box sx={{width:36,height:36,borderRadius:2,bgcolor:String(x.status).toUpperCase()==='PRESENT'?'#e8f8f5':'#fff1f2',display:'grid',placeItems:'center'}}><EventAvailableRounded fontSize="small" color={String(x.status).toUpperCase()==='PRESENT'?'success':'error'}/></Box><Box sx={{flex:1}}><Typography sx={{fontWeight:800,fontSize:13}}>{x.subject_name}</Typography><Typography sx={{fontSize:11.5,color:'#64748b'}}>{date(x.session_date)} · {x.start_time||'Time not set'}</Typography></Box><Chip size="small" label={x.status} color={String(x.status).toUpperCase()==='PRESENT'?'success':'default'} variant="outlined"/></Stack>)}{!(data.attendanceRecent||[]).length&&<Empty title="No attendance records" text="Attendance will appear here when your institute records sessions."/>}</Stack>
  </CardContent></Card>
 </Stack>
}

function Homework({data,isStudent,onRefresh}:{data:any;isStudent?:boolean;onRefresh:()=>void}){
 const [busy,setBusy]=useState<string|null>(null);
 const submit=async(id:string)=>{setBusy(id);try{await api.post('/portal/student/'+data.student.id+'/homework/'+id+'/submit');onRefresh()}catch(e:any){alert(e?.response?.data?.message||'Unable to submit homework')}finally{setBusy(null)}};
 return <Stack spacing={1.5}>{(data.homework||[]).map((x:any)=><Card key={x.id}><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2}><Box sx={{width:42,height:42,borderRadius:2,bgcolor:'#eef8f6',display:'grid',placeItems:'center',flexShrink:0}}><AssignmentRounded sx={{color:'#0f766e'}}/></Box><Box sx={{flex:1}}><Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap"><Typography sx={{fontWeight:900}}>{x.title}</Typography><Chip size="small" label={x.submission_status} color={['SUBMITTED','GRADED'].includes(String(x.submission_status).toUpperCase())?'success':'default'} variant="outlined"/></Stack><Typography sx={{fontSize:12.5,color:'#64748b',mt:.6}}>{x.description||'No description provided.'}</Typography><Typography sx={{fontSize:11.5,color:'#94a3b8',mt:1}}>Due: {dateTime(x.due_at)} · Teacher: {x.teacher_first_name||'—'} {x.teacher_last_name||''}</Typography>{x.feedback&&<Alert severity="info" sx={{mt:1}}>Feedback: {x.feedback}</Alert>}</Box>{isStudent&&String(x.submission_status).toUpperCase()==='PENDING'&&<Button variant="contained" endIcon={<ArrowForwardRounded/>} disabled={busy===x.id} onClick={()=>submit(x.id)}>{busy===x.id?'Submitting…':'Mark submitted'}</Button>}</Stack></CardContent></Card>)}{!(data.homework||[]).length&&<Empty title="No homework assigned" text="New assignments from your batch will appear here."/>}</Stack>
}

function Results({data}:{data:any}){
 return <Stack spacing={1.5}>{(data.results||[]).map((x:any)=><Card key={x.id}><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Box sx={{width:48,height:48,borderRadius:2,bgcolor:'#eef8f6',display:'grid',placeItems:'center'}}><SchoolRounded sx={{color:'#0f766e'}}/></Box><Box sx={{flex:1}}><Typography sx={{fontWeight:900}}>{x.exam_name}</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>Published {dateTime(x.published_at)}</Typography></Box><Box sx={{textAlign:{sm:'right'}}}><Typography sx={{fontSize:25,fontWeight:950,color:'#0f766e'}}>{x.percentage??0}%</Typography><Typography sx={{fontSize:11.5,color:'#64748b'}}>{x.obtained_marks??0}/{x.total_marks??0} · Rank {x.rank??'—'}</Typography></Box></Stack></CardContent></Card>)}{!(data.results||[]).length&&<Empty title="No published results yet" text="Your results will appear here after the institute publishes them."/>}</Stack>
}

function Fees({data}:{data:any}){
 const total=(data.fees||[]).reduce((a:any,x:any)=>a+Number(x.amount||0),0),paid=(data.fees||[]).reduce((a:any,x:any)=>a+Number(x.paid_amount||0),0);
 return <Stack spacing={2}><Card><CardContent><SectionTitle title="Fee summary" text="Invoice ledger for your account"/><Stack direction={{xs:'column',sm:'row'}} spacing={3} sx={{mt:2}}><Row label="Total billed" value={money(total)}/><Row label="Paid" value={money(paid)}/><Row label="Outstanding" value={money(Math.max(total-paid,0))}/></Stack></CardContent></Card>
 {(data.fees||[]).map((x:any)=><Card key={x.invoice_number}><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={1.5} alignItems={{sm:'center'}}><Box sx={{flex:1}}><Typography sx={{fontWeight:900}}>{x.invoice_number}</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>Due {date(x.due_date)}</Typography></Box><Typography sx={{fontWeight:850}}>{money(x.paid_amount)} / {money(x.amount)}</Typography><Chip label={x.status} size="small" variant="outlined"/></Stack></CardContent></Card>)}
 {!data.fees?.length&&<Empty title="No invoices" text="Fee invoices will appear here when issued by the institute."/>}</Stack>
}

function Schedule({data}:{data:any}){
 const groups=useMemo(()=>{const m:any={};(data.timetable||[]).forEach((x:any)=>(m[x.day_of_week]??=[]).push(x));return m},[data.timetable]);
 return <Stack spacing={1.5}>{Object.keys(groups).sort((a,b)=>Number(a)-Number(b)).map(day=><Card key={day}><CardContent><Typography sx={{fontWeight:900,mb:1.2}}>{dayName(day)}</Typography><Stack divider={<Divider/>}>{groups[day].map((x:any,i:number)=><Stack key={i} direction="row" spacing={1.5} sx={{py:1.2,alignItems:'center'}}><ScheduleRounded sx={{color:'#0f766e'}}/><Box sx={{flex:1}}><Typography sx={{fontWeight:800,fontSize:13}}>{x.subject_name}</Typography><Typography sx={{fontSize:11.5,color:'#64748b'}}>{x.teacher_name||'Teacher not assigned'} · {x.classroom_id||'Room not assigned'}</Typography></Box><Typography sx={{fontSize:12,fontWeight:800}}>{x.start_time}–{x.end_time}</Typography></Stack>)}</Stack></CardContent></Card>)}{!(data.timetable||[]).length&&<Empty title="No timetable published" text="Your batch timetable will appear here once configured."/>}</Stack>
}

function Exams({data,studentId,onOpen}:{data:any;studentId:string;onOpen:(x:any)=>void}){
 return <Stack spacing={1.5}>{(data.exams||[]).map((x:any)=><Card key={x.id}><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Box sx={{width:46,height:46,borderRadius:2,bgcolor:'#eef8f6',display:'grid',placeItems:'center'}}><QuizRounded sx={{color:'#0f766e'}}/></Box><Box sx={{flex:1}}><Typography sx={{fontWeight:900}}>{x.name}</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>{x.exam_type} · {x.status}</Typography><Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.5}}>{dateTime(x.starts_at)} → {dateTime(x.ends_at)}</Typography></Box><Button variant="contained" onClick={()=>onOpen({exam:x,studentId})}>Open exam</Button></Stack></CardContent></Card>)}{!(data.exams||[]).length&&<Empty title="No upcoming exams" text="Scheduled exams will appear here."/>}</Stack>
}

function Notifications({data}:{data:any}){return <Stack spacing={1.25}>{(data.notifications||[]).map((x:any)=><Card key={x.id} sx={{borderLeft:x.read_at?'1px solid #e8edf3':'4px solid #0f766e'}}><CardContent><Stack direction="row" spacing={1.5}><NotificationsRounded sx={{color:'#0f766e'}}/><Box><Typography sx={{fontWeight:900}}>{x.title}</Typography><Typography sx={{fontSize:12.5,color:'#64748b',mt:.4}}>{x.body||'No additional details.'}</Typography><Typography sx={{fontSize:10.5,color:'#94a3b8',mt:1}}>{dateTime(x.created_at)}</Typography></Box></Stack></CardContent></Card>)}{!(data.notifications||[]).length&&<Empty title="You're all caught up" text="New institute notifications will appear here."/>}</Stack>}

function ExamDialog({exam,studentId,onClose,onSubmitted}:{exam:any;studentId:string;onClose:()=>void;onSubmitted:()=>void}){
 const [workspace,setWorkspace]=useState<any>(),[answers,setAnswers]=useState<any>({}),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const start=async()=>{setBusy(true);setError('');try{const r=await api.post('/exams/'+exam.exam.id+'/attempts',{studentId});const a=unwrap(r);const w=unwrap(await api.get('/exams/'+exam.exam.id+'/attempts/'+a.attemptId));setWorkspace(w)}catch(e:any){setError(e?.response?.data?.message||'Unable to start exam')}finally{setBusy(false)}};
 useEffect(()=>{start()},[]);
 const submit=async()=>{setBusy(true);setError('');try{await api.post('/exams/attempts/'+workspace.attempt.id+'/submit',{answers});onSubmitted()}catch(e:any){setError(e?.response?.data?.message||'Unable to submit exam')}finally{setBusy(false)}};
 return <Dialog open onClose={busy?undefined:onClose} fullWidth maxWidth="md"><DialogTitle>{workspace?.exam?.name||exam.exam.name}</DialogTitle><DialogContent dividers>
  {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
  {!workspace?<Stack sx={{py:6,alignItems:'center'}}><LinearProgress sx={{width:'100%',mb:2}}/><Typography>{busy?'Preparing your exam…':'Loading…'}</Typography></Stack>:
   <Stack spacing={2}>{workspace.questions.map((q:any,i:number)=><Card key={q.id} variant="outlined"><CardContent><Typography sx={{fontWeight:850}}>Q{i+1}. {q.text}</Typography><Typography sx={{fontSize:11,color:'#94a3b8',mt:.4}}>{q.question_type} · {q.marks} marks</Typography>
    {q.question_type==='MCQ_SINGLE'||q.question_type==='TRUE_FALSE'?<RadioGroup value={answers[q.id]||''} onChange={e=>setAnswers((a:any)=>({...a,[q.id]:e.target.value}))}>{q.options.map((o:any)=><FormControlLabel key={o.id} value={o.option_text} control={<Radio/>} label={o.option_text}/>)}</RadioGroup>:
    q.question_type==='MCQ_MULTI'?<Stack>{q.options.map((o:any)=><FormControlLabel key={o.id} control={<Checkbox checked={String(answers[q.id]||'').split(',').filter(Boolean).includes(o.option_text)} onChange={e=>{const cur=String(answers[q.id]||'').split(',').filter(Boolean);const next=e.target.checked?[...cur,o.option_text]:cur.filter(v=>v!==o.option_text);setAnswers((a:any)=>({...a,[q.id]:next.join(',')}))}}/>} label={o.option_text}/>)}</Stack>:
    <TextField fullWidth size="small" placeholder="Type your answer" value={answers[q.id]||''} onChange={e=>setAnswers((a:any)=>({...a,[q.id]:e.target.value}))}/>}
   </CardContent></Card>)}</Stack>}
 </DialogContent><DialogActions><Button onClick={onClose} disabled={busy}>Cancel</Button>{workspace&&<Button variant="contained" onClick={submit} disabled={busy}>{busy?'Submitting…':'Submit exam'}</Button>}</DialogActions></Dialog>
}

function Kpi({icon:Icon,label,value,sub}:{icon?:any;label:string;value:any;sub?:string}){return <Card><CardContent sx={{p:{xs:1.75,md:2.25}}}>{Icon&&<Icon sx={{fontSize:20,color:'#0f766e'}}/>}<Typography sx={{fontSize:11,color:'#64748b',mt:Icon?1:0,textTransform:'uppercase',letterSpacing:.3}}>{label}</Typography><Typography sx={{fontSize:{xs:21,md:25},fontWeight:950,mt:.2}}>{value}</Typography>{sub&&<Typography sx={{fontSize:10.5,color:'#94a3b8',mt:.2}}>{sub}</Typography>}</CardContent></Card>}
function SectionTitle({title,text}:{title:string;text?:string}){return <Box sx={{mb:1.5}}><Typography sx={{fontWeight:900,fontSize:15}}>{title}</Typography>{text&&<Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.25}}>{text}</Typography>}</Box>}
function ActionRow({icon:Icon,title,text,onClick,badge}:{icon:any;title:string;text:string;onClick:()=>void;badge:string}){return <Button onClick={onClick} sx={{justifyContent:'flex-start',textAlign:'left',p:1.25,border:'1px solid #edf1f5',borderRadius:2,color:'#0f172a'}}><Icon sx={{color:'#0f766e',mr:1.25}}/><Box sx={{flex:1}}><Typography sx={{fontSize:12.5,fontWeight:800}}>{title}</Typography><Typography sx={{fontSize:10.5,color:'#94a3b8'}}>{text}</Typography></Box><Chip size="small" label={badge}/></Button>}
function Row({label,value}:{label:string;value:any}){return <Box><Typography sx={{fontSize:10.5,color:'#94a3b8'}}>{label}</Typography><Typography sx={{fontSize:13,fontWeight:800,mt:.15}}>{value}</Typography></Box>}
function Empty({title,text}:{title:string;text:string}){return <Card><CardContent sx={{py:7,textAlign:'center'}}><CheckCircleRounded sx={{fontSize:34,color:'#0f766e',mb:1}}/><Typography sx={{fontWeight:900}}>{title}</Typography><Typography sx={{fontSize:12.5,color:'#64748b',mt:.5}}>{text}</Typography></CardContent></Card>}
