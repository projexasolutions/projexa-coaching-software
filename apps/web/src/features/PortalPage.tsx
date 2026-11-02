import {useEffect,useMemo,useState} from 'react';
import {Alert,Avatar,Box,Button,Card,CardContent,Checkbox,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Divider,FormControlLabel,LinearProgress,MenuItem,Radio,RadioGroup,Select,Stack,Tab,Tabs,TextField,Typography} from '@mui/material';
import {AssignmentRounded,EventAvailableRounded,NotificationsRounded,PaymentsRounded,QuizRounded,SchoolRounded,ArrowForwardRounded,CheckCircleRounded,ScheduleRounded,TodayRounded,CampaignRounded} from '@mui/icons-material';
import {api} from '../services';
import {useAuth} from './auth/authStore';

function unwrap(r:any){return r?.data?.data??r?.data??{}}
const money=(n:any)=>'₹'+Number(n||0).toLocaleString('en-IN',{maximumFractionDigits:2});
const date=(v:any)=>v?new Date(v).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'}):'—';
const dateTime=(v:any)=>v?new Date(v).toLocaleString('en-IN',{day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}):'—';
const dayName=(n:any)=>['','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'][Number(n)]||'—';
const todayIndex=()=>{const d=new Date().getDay();return d===0?7:d};
const greeting=()=>{const h=new Date().getHours();return h<12?'Good morning':h<17?'Good afternoon':'Good evening'};
const asMinutes=(v:any)=>{if(!v)return 0;const [h,m]=String(v).split(':').map(Number);return (h||0)*60+(m||0)};

export default function PortalPage(){
 const session=useAuth(s=>s.session);
 const [children,setChildren]=useState<any[]>([]),[id,setId]=useState(''),[data,setData]=useState<any>(),[error,setError]=useState(''),[loading,setLoading]=useState(true),[studentLoading,setStudentLoading]=useState(false),[tab,setTab]=useState(0),[exam,setExam]=useState<any>(null);
 const isStudent=session?.roles?.includes('STUDENT');
 const load=async()=>{
  setLoading(true);setError('');
  try{const d=unwrap(await api.get('/portal/me'));setChildren(d.children||[]);if(!id&&d.children?.[0]?.id)setId(d.children[0].id);}
  catch(e:any){setError(e?.response?.data?.message||'Unable to load portal')}finally{setLoading(false)}
 };
 const loadStudent=async(sid=id)=>{
  if(!sid)return;
  setStudentLoading(true);
  try{setError('');setData(unwrap(await api.get('/portal/student/'+sid)));}
  catch(e:any){setError(e?.response?.data?.message||'Unable to load student portal')}
  finally{setStudentLoading(false)}
 };
 useEffect(()=>{load()},[]);
 useEffect(()=>{if(id){setData(undefined);loadStudent(id)}},[id]);
 if(loading&&!children.length)return <Card><CardContent sx={{py:10,textAlign:'center'}}><LinearProgress sx={{mb:3}}/><Typography sx={{color:'#64748b'}}>Loading your learning workspace…</Typography></CardContent></Card>;
 if(error&&!data)return <Stack spacing={1.5}><Alert severity="error" sx={{borderRadius:3}}>{error}</Alert><Button variant="outlined" onClick={()=>id?loadStudent(id):load()}>Retry loading portal</Button></Stack>;
 if(!children.length)return <Empty title="No linked student profile" text="Your institute has not linked a student to this portal account yet."/>;

 return <Stack spacing={2.25} sx={{width:'100%',maxWidth:1440,mx:'auto',pb:3}}>
  <WelcomeHeader data={data}/>
  <Hero data={data} childrenRows={children} id={id} setId={setId} isStudent={isStudent}/>
  {error&&<Alert severity="warning" onClose={()=>setError('')}>{error}</Alert>}
  <Tabs value={tab} onChange={(_,v)=>setTab(v)} variant="scrollable" allowScrollButtonsMobile sx={{bgcolor:'#fff',border:'1px solid #e8edf3',borderRadius:2.5,px:1}}>
   {['Overview','Attendance','Homework','Results','Fees','Schedule','Exams','Notifications'].map(x=><Tab key={x} label={x} sx={{fontWeight:800,fontSize:12}}/>)}
  </Tabs>
  {!data||studentLoading?<Card><CardContent sx={{py:8,textAlign:'center'}}><LinearProgress sx={{mb:2}}/><Typography color="text.secondary">Loading your learning data…</Typography></CardContent></Card>:
   <>
    {tab===0&&<Overview data={data} setTab={setTab}/>}
    {tab===1&&<Attendance data={data}/>}
    {tab===2&&<Homework data={data} isStudent={isStudent} onRefresh={()=>loadStudent(id)}/>}
    {tab===3&&<Results data={data}/>}
    {tab===4&&<Fees data={data}/>}
    {tab===5&&<Schedule data={data}/>}
    {tab===6&&<Exams data={data} studentId={id} isStudent={isStudent} onOpen={setExam}/>}
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

function WelcomeHeader({data}:{data:any}){
 const s=data?.student;const now=new Date();
 const label=now.toLocaleDateString('en-IN',{weekday:'long',day:'numeric',month:'long',year:'numeric'});
 return <Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" alignItems={{sm:'flex-end'}} spacing={1}>
  <Box><Typography sx={{fontSize:{xs:23,md:28},fontWeight:950,letterSpacing:-.8,color:'#0f172a'}}>{greeting()}{s?.first_name?', '+s.first_name:''} 👋</Typography><Typography sx={{fontSize:13,color:'#64748b',mt:.35}}>Here's your learning overview for today.</Typography></Box>
  <Chip icon={<TodayRounded/>} label={label} variant="outlined" sx={{alignSelf:{xs:'flex-start',sm:'auto'},bgcolor:'#fff',borderColor:'#e2e8f0',fontWeight:700}}/>
 </Stack>
}
function Overview({data,setTab}:{data:any;setTab:(n:number)=>void}){
 const attendance=data.attendance||[],total=attendance.reduce((a:any,x:any)=>a+Number(x.total||0),0),present=Number(attendance.find((x:any)=>String(x.status).toUpperCase()==='PRESENT')?.total||0);
 const pct=total?Math.round(present/total*100):null;
 const due=(data.fees||[]).reduce((a:any,x:any)=>a+Math.max(Number(x.balance??(Number(x.amount||0)-Number(x.paid_amount||0))),0),0);
 const pending=(data.homework||[]).filter((x:any)=>!['SUBMITTED','GRADED'].includes(String(x.submission_status).toUpperCase()));
 const today=todayIndex();
 const todayClasses=(data.timetable||[]).filter((x:any)=>Number(x.day_of_week)===today).sort((a:any,b:any)=>asMinutes(a.start_time)-asMinutes(b.start_time));
 const upcoming=(data.exams||[]).filter((x:any)=>!x.starts_at||new Date(x.starts_at)>=new Date()).sort((a:any,b:any)=>new Date(a.starts_at||0).getTime()-new Date(b.starts_at||0).getTime()).slice(0,3);
 const latest=(data.results||[]).slice(0,3),updates=(data.notifications||[]).slice(0,3),p=data.enrollment||{};
 return <Stack spacing={2}>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'repeat(2,minmax(0,1fr))',lg:'repeat(4,minmax(0,1fr))'},gap:1.5}}>
   <Kpi icon={EventAvailableRounded} label="Attendance" value={pct===null?'—':pct+'%'} sub={total?present+' of '+total+' sessions present':'No attendance recorded'}/>
   <Kpi icon={AssignmentRounded} label="Homework" value={pending.length} sub={pending.length?'Needs your attention':'All assigned work is up to date'}/>
   <Kpi icon={QuizRounded} label="Results" value={data.results?.length||0} sub={data.results?.length?'Published results':'No results published yet'}/>
   <Kpi icon={PaymentsRounded} label="Fees due" value={money(due)} sub={due>0?'Outstanding balance':'No outstanding balance'}/>
  </Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'1.2fr .8fr'},gap:1.5}}>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Today's schedule" text={todayClasses.length?'Your classes planned for today':'Your batch timetable at a glance'}/>
    {todayClasses.length?<Stack divider={<Divider/>}>{todayClasses.map((x:any,i:number)=><Stack key={x.id||i} direction="row" spacing={1.5} alignItems="center" sx={{py:1.1}}>
     <Box sx={{minWidth:74,bgcolor:'#eff8f6',borderRadius:2,p:1,textAlign:'center'}}><Typography sx={{fontSize:12,fontWeight:900,color:'#0f766e'}}>{String(x.start_time||'').slice(0,5)||'—'}</Typography><Typography sx={{fontSize:10,color:'#64748b'}}>{String(x.end_time||'').slice(0,5)}</Typography></Box>
     <Box sx={{flex:1,minWidth:0}}><Typography sx={{fontSize:13,fontWeight:850}}>{x.subject_name||'Class session'}</Typography><Typography sx={{fontSize:11.5,color:'#64748b'}}>{x.teacher_name||'Teacher not assigned'} · {x.classroom_id||'Room not assigned'}</Typography></Box><Chip size="small" label="Class" variant="outlined"/>
    </Stack>)}</Stack>:<EmptyInline title="No classes scheduled today" text="Your institute has not published a class for today."/>}
    <Button size="small" endIcon={<ArrowForwardRounded/>} onClick={()=>setTab(5)} sx={{mt:1}}>View full timetable</Button>
   </CardContent></Card>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Academic profile" text="Your current enrollment details"/>
    <Box sx={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:2}}>
     <Row label="Academic year" value={p.academic_year_name||'—'}/><Row label="Class" value={p.class_name||'—'}/>
     <Row label="Stream" value={p.stream_name||'—'}/><Row label="Batch" value={p.batch_name||'—'}/>
     <Row label="Admission no." value={data.student?.admission_number||'—'}/><Row label="Enrollment" value={p.status||'Active'}/>
    </Box>
   </CardContent></Card>
  </Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'1fr 1fr'},gap:1.5}}>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Your action center" text="A quick view of items that may need your attention."/>
    <Stack spacing={1}>
     <ActionRow icon={AssignmentRounded} title={pending.length?pending.length+' homework item'+(pending.length>1?'s':'')+' pending':'Homework is up to date'} text={pending[0]?.due_at?'Next due '+dateTime(pending[0].due_at):'View assignments and submission status'} onClick={()=>setTab(2)} badge={pending.length?String(pending.length):'✓'}/>
     <ActionRow icon={EventAvailableRounded} title={pct===null?'Attendance not recorded':pct+'% attendance'} text={total?'Based on '+total+' recorded sessions':'Attendance history will appear here'} onClick={()=>setTab(1)} badge={pct===null?'View':pct+'%'}/>
     <ActionRow icon={PaymentsRounded} title={due>0?'Fee payment pending':'Fees are up to date'} text={due>0?'Outstanding '+money(due):'Review your fee statement'} onClick={()=>setTab(4)} badge={due>0?'Due':'✓'}/>
     <ActionRow icon={QuizRounded} title={upcoming.length?upcoming.length+' upcoming exam'+(upcoming.length>1?'s':''):'No upcoming exams'} text={upcoming[0]?.starts_at?'Next: '+dateTime(upcoming[0].starts_at):'Review examination schedule'} onClick={()=>setTab(6)} badge="View"/>
    </Stack>
   </CardContent></Card>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Upcoming examinations" text="Prepare ahead for your next assessments."/>
    {upcoming.length?<Stack divider={<Divider/>}>{upcoming.map((x:any)=><Stack key={x.id} direction="row" spacing={1.5} alignItems="center" sx={{py:1.1}}>
     <Box sx={{width:42,height:42,borderRadius:2,bgcolor:'#eff8f6',display:'grid',placeItems:'center',flexShrink:0}}><QuizRounded sx={{color:'#0f766e'}}/></Box>
     <Box sx={{flex:1,minWidth:0}}><Typography sx={{fontSize:13,fontWeight:850}}>{x.name}</Typography><Typography sx={{fontSize:11.5,color:'#64748b'}}>{x.exam_type||'Examination'} · {dateTime(x.starts_at)}</Typography></Box><Chip size="small" label={x.status||'Scheduled'} variant="outlined"/>
    </Stack>)}</Stack>:<EmptyInline title="No upcoming exams" text="New examinations will appear here when scheduled."/>}
    <Button size="small" endIcon={<ArrowForwardRounded/>} onClick={()=>setTab(6)} sx={{mt:1}}>View all exams</Button>
   </CardContent></Card>
  </Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'1fr 1fr'},gap:1.5}}>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Recent results" text="Your latest published academic performance."/>
    {latest.length?<Stack spacing={1.5}>{latest.map((x:any)=><Box key={x.id}><Stack direction="row" justifyContent="space-between" alignItems="center" mb={.6}><Box><Typography sx={{fontSize:12.5,fontWeight:850}}>{x.exam_name||'Assessment'}</Typography><Typography sx={{fontSize:10.5,color:'#94a3b8'}}>{date(x.published_at)}</Typography></Box><Typography sx={{fontSize:16,fontWeight:950,color:'#0f766e'}}>{x.percentage??'—'}{x.percentage!=null?'%':''}</Typography></Stack><LinearProgress variant="determinate" value={Math.max(0,Math.min(100,Number(x.percentage)||0))} sx={{height:7,borderRadius:8,bgcolor:'#edf2f7'}}/></Box>)}</Stack>:<EmptyInline title="Results will appear here" text="Your performance summary updates after results are published."/>}
    <Button size="small" endIcon={<ArrowForwardRounded/>} onClick={()=>setTab(3)} sx={{mt:1}}>View all results</Button>
   </CardContent></Card>
   <Card><CardContent sx={{p:{xs:2,md:2.5}}}><SectionTitle title="Institute updates" text="Announcements and notifications from your institute."/>
    {updates.length?<Stack divider={<Divider/>}>{updates.map((x:any)=><Stack key={x.id} direction="row" spacing={1.2} alignItems="flex-start" sx={{py:1}}>
     <CampaignRounded sx={{color:'#0f766e',mt:.2}}/><Box sx={{flex:1,minWidth:0}}><Typography sx={{fontSize:12.5,fontWeight:850}}>{x.title}</Typography><Typography sx={{fontSize:11.5,color:'#64748b',mt:.25,display:'-webkit-box',WebkitLineClamp:2,WebkitBoxOrient:'vertical',overflow:'hidden'}}>{x.body||'Open to view the update.'}</Typography><Typography sx={{fontSize:10,color:'#94a3b8',mt:.5}}>{dateTime(x.created_at)}</Typography></Box>{!x.read_at&&<Chip size="small" label="New" color="success"/>}
    </Stack>)}</Stack>:<EmptyInline title="You're all caught up" text="Important institute updates will appear here."/>}
    <Button size="small" endIcon={<ArrowForwardRounded/>} onClick={()=>setTab(7)} sx={{mt:1}}>View notifications</Button>
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

function Exams({data,studentId,isStudent,onOpen}:{data:any;studentId:string;isStudent?:boolean;onOpen:(x:any)=>void}){
 return <Stack spacing={1.5}>{(data.exams||[]).map((x:any)=><Card key={x.id}><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Box sx={{width:46,height:46,borderRadius:2,bgcolor:'#eef8f6',display:'grid',placeItems:'center'}}><QuizRounded sx={{color:'#0f766e'}}/></Box><Box sx={{flex:1}}><Typography sx={{fontWeight:900}}>{x.name}</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>{x.exam_type} · {x.status}</Typography><Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.5}}>{dateTime(x.starts_at)} → {dateTime(x.ends_at)}</Typography></Box>{isStudent?<Button variant="contained" onClick={()=>onOpen({exam:x,studentId})}>Start exam</Button>:<Chip label="Scheduled" variant="outlined"/>}</Stack></CardContent></Card>)}{!(data.exams||[]).length&&<Empty title="No upcoming exams" text="Scheduled exams will appear here."/>}</Stack>
}

function Notifications({data}:{data:any}){return <Stack spacing={1.25}>{(data.notifications||[]).map((x:any)=><Card key={x.id} sx={{borderLeft:x.read_at?'1px solid #e8edf3':'4px solid #0f766e'}}><CardContent><Stack direction="row" spacing={1.5}><NotificationsRounded sx={{color:'#0f766e'}}/><Box><Typography sx={{fontWeight:900}}>{x.title}</Typography><Typography sx={{fontSize:12.5,color:'#64748b',mt:.4}}>{x.body||'No additional details.'}</Typography><Typography sx={{fontSize:10.5,color:'#94a3b8',mt:1}}>{dateTime(x.created_at)}</Typography></Box></Stack></CardContent></Card>)}{!(data.notifications||[]).length&&<Empty title="You're all caught up" text="New institute notifications will appear here."/>}</Stack>}

function ExamDialog({exam,studentId,onClose,onSubmitted}:{exam:any;studentId:string;onClose:()=>void;onSubmitted:()=>void}){
 const [workspace,setWorkspace]=useState<any>(),[answers,setAnswers]=useState<any>({}),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const start=async()=>{setBusy(true);setError('');try{const r=await api.post('/exams/'+exam.exam.id+'/attempts',{studentId});const a=unwrap(r);const w=unwrap(await api.get('/exams/'+exam.exam.id+'/attempts/'+a.attemptId));setWorkspace(w)}catch(e:any){setError(e?.response?.data?.message||'Unable to start exam')}finally{setBusy(false)}};
 useEffect(()=>{start()},[]);
 const submit=async()=>{setBusy(true);setError('');try{await api.post('/exams/attempts/'+workspace.attempt.id+'/submit',{answers});onSubmitted()}catch(e:any){setError(e?.response?.data?.message||'Unable to submit exam')}finally{setBusy(false)}};
 return <Dialog open onClose={busy?undefined:onClose} fullWidth maxWidth="md"><DialogTitle>{workspace?.exam?.name||exam.exam.name}</DialogTitle><DialogContent dividers>
  {error&&<Stack spacing={1} sx={{mb:2}}><Alert severity="error">{error}</Alert>{!workspace&&<Button variant="outlined" onClick={start} disabled={busy}>Retry loading exam</Button>}</Stack>}
  {!workspace?<Stack sx={{py:6,alignItems:'center'}}><LinearProgress sx={{width:'100%',mb:2}}/><Typography>{busy?'Preparing your exam…':'Loading…'}</Typography></Stack>:
   <Stack spacing={2}>{(workspace.questions||[]).map((q:any,i:number)=><Card key={q.id} variant="outlined"><CardContent><Typography sx={{fontWeight:850}}>Q{i+1}. {q.text}</Typography><Typography sx={{fontSize:11,color:'#94a3b8',mt:.4}}>{q.question_type} · {q.marks} marks</Typography>
    {q.question_type==='MCQ_SINGLE'||q.question_type==='TRUE_FALSE'?<RadioGroup value={answers[q.id]||''} onChange={e=>setAnswers((a:any)=>({...a,[q.id]:e.target.value}))}>{q.options.map((o:any)=><FormControlLabel key={o.id} value={o.option_text} control={<Radio/>} label={o.option_text}/>)}</RadioGroup>:
    q.question_type==='MCQ_MULTI'?<Stack>{q.options.map((o:any)=><FormControlLabel key={o.id} control={<Checkbox checked={String(answers[q.id]||'').split(',').filter(Boolean).includes(o.option_text)} onChange={e=>{const cur=String(answers[q.id]||'').split(',').filter(Boolean);const next=e.target.checked?[...cur,o.option_text]:cur.filter(v=>v!==o.option_text);setAnswers((a:any)=>({...a,[q.id]:next.join(',')}))}}/>} label={o.option_text}/>)}</Stack>:
    <TextField fullWidth size="small" placeholder="Type your answer" value={answers[q.id]||''} onChange={e=>setAnswers((a:any)=>({...a,[q.id]:e.target.value}))}/>}
   </CardContent></Card>)}</Stack>}
  {workspace&&!(workspace.questions||[]).length&&<Alert severity="warning">No questions are available for this exam. Contact your institute before submitting.</Alert>}
 </DialogContent><DialogActions><Button onClick={onClose} disabled={busy}>Cancel</Button>{workspace&&<Button variant="contained" onClick={()=>{if(window.confirm('Submit your exam now? You may not be able to change your answers afterward.'))submit()}} disabled={busy||!(workspace.questions||[]).length}>{busy?'Submitting…':'Submit exam'}</Button>}</DialogActions></Dialog>
}

function Kpi({icon:Icon,label,value,sub}:{icon?:any;label:string;value:any;sub?:string}){return <Card sx={{height:'100%',borderRadius:3,border:'1px solid #e8edf3',boxShadow:'0 2px 8px rgba(15,23,42,.025)'}}><CardContent sx={{p:{xs:1.6,md:2.1}}}><Stack direction="row" alignItems="center" justifyContent="space-between"><Typography sx={{fontSize:10.5,color:'#64748b',fontWeight:850,textTransform:'uppercase',letterSpacing:.45}}>{label}</Typography>{Icon&&<Box sx={{width:34,height:34,borderRadius:2,bgcolor:'#eaf7f5',display:'grid',placeItems:'center'}}><Icon sx={{fontSize:18,color:'#0f766e'}}/></Box>}</Stack><Typography sx={{fontSize:{xs:21,md:26},fontWeight:950,mt:1,color:'#0f172a',letterSpacing:-.5,overflowWrap:'anywhere'}}>{value}</Typography>{sub&&<Typography sx={{fontSize:10.5,color:'#94a3b8',mt:.3}}>{sub}</Typography>}</CardContent></Card>}
function SectionTitle({title,text}:{title:string;text?:string}){return <Box sx={{mb:1.5}}><Typography sx={{fontWeight:900,fontSize:15}}>{title}</Typography>{text&&<Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.25}}>{text}</Typography>}</Box>}
function ActionRow({icon:Icon,title,text,onClick,badge}:{icon:any;title:string;text:string;onClick:()=>void;badge:string}){return <Button onClick={onClick} sx={{justifyContent:'flex-start',textAlign:'left',p:1.25,border:'1px solid #edf1f5',borderRadius:2,color:'#0f172a'}}><Icon sx={{color:'#0f766e',mr:1.25}}/><Box sx={{flex:1}}><Typography sx={{fontSize:12.5,fontWeight:800}}>{title}</Typography><Typography sx={{fontSize:10.5,color:'#94a3b8'}}>{text}</Typography></Box><Chip size="small" label={badge}/></Button>}
function Row({label,value}:{label:string;value:any}){return <Box><Typography sx={{fontSize:10.5,color:'#94a3b8'}}>{label}</Typography><Typography sx={{fontSize:13,fontWeight:800,mt:.15}}>{value}</Typography></Box>}
function EmptyInline({title,text}:{title:string;text:string}){return <Box sx={{py:2.5,textAlign:'center',bgcolor:'#f8fafc',borderRadius:2.5,border:'1px dashed #e2e8f0'}}><CheckCircleRounded sx={{fontSize:27,color:'#0f766e',mb:.6}}/><Typography sx={{fontWeight:850,fontSize:12.5}}>{title}</Typography><Typography sx={{fontSize:11.5,color:'#64748b',mt:.35}}>{text}</Typography></Box>}
function Empty({title,text}:{title:string;text:string}){return <Card><CardContent sx={{py:7,textAlign:'center'}}><CheckCircleRounded sx={{fontSize:34,color:'#0f766e',mb:1}}/><Typography sx={{fontWeight:900}}>{title}</Typography><Typography sx={{fontSize:12.5,color:'#64748b',mt:.5}}>{text}</Typography></CardContent></Card>}
