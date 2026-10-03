import {useEffect,useMemo,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,FormControl,Grid,IconButton,InputLabel,MenuItem,Select,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Tooltip,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,EditRounded,ScheduleRounded} from '@mui/icons-material';
import {api} from '../services';

type Resource={id:string;name:string;[key:string]:any};
type Entry={id:string;batch_id:string;subject_id?:string;teacher_id?:string;classroom_id?:string;day_of_week:number;start_time:string;end_time:string;batch_name:string;subject_name:string;teacher_name?:string;classroom_name?:string};
const days=[{v:1,n:'Monday'},{v:2,n:'Tuesday'},{v:3,n:'Wednesday'},{v:4,n:'Thursday'},{v:5,n:'Friday'},{v:6,n:'Saturday'},{v:7,n:'Sunday'}];

function unwrap(r:any){return r?.data?.data??r?.data??[]}
function dayName(n:number){return days.find(d=>d.v===n)?.n??'Day'}

export default function TimetablePage(){
 const [entries,setEntries]=useState<Entry[]>([]),[batches,setBatches]=useState<Resource[]>([]),[subjects,setSubjects]=useState<Resource[]>([]),[teachers,setTeachers]=useState<Resource[]>([]),[classrooms,setClassrooms]=useState<Resource[]>([]);
 const [batchId,setBatchId]=useState(''),[day,setDay]=useState(0),[open,setOpen]=useState(false),[editing,setEditing]=useState<Entry|null>(null),[error,setError]=useState('');
 const blank={batchId:'',subjectId:'',teacherId:'',classroomId:'',dayOfWeek:1,startTime:'08:00',endTime:'09:00'};
 const [form,setForm]=useState<any>(blank);

 async function load(){
   try{
     setError('');
     const [t,b]=await Promise.all([api.get('/timetable',{params:{batchId:batchId||undefined,dayOfWeek:day||undefined}}),api.get('/timetable/resources')]);
     setEntries(unwrap(t)); const rr=unwrap(b);
     setBatches(rr.batches??[]);setSubjects(rr.subjects??[]);setTeachers(rr.teachers??[]);setClassrooms(rr.classrooms??[]);
   }catch(e:any){setError(e?.response?.data?.message||e?.response?.data?.error?.message||'Unable to load timetable');}
 }
 useEffect(()=>{load()},[batchId,day]);

 const grouped=useMemo(()=>days.map(d=>({day:d,...entries.filter(e=>e.day_of_week===d.v)})).filter(x=>x.entries.length),[entries]);

 function startCreate(){setEditing(null);setForm({...blank,batchId:batchId||batches[0]?.id||'',dayOfWeek:day||1});setOpen(true);setError('')}
 function startEdit(e:Entry){setEditing(e);setForm({batchId:e.batch_id,subjectId:e.subject_id||'',teacherId:e.teacher_id||'',classroomId:e.classroom_id||'',dayOfWeek:e.day_of_week,startTime:String(e.start_time).slice(0,5),endTime:String(e.end_time).slice(0,5)});setOpen(true);setError('')}
 async function save(){
   try{
     setError('');
     const payload={...form,subjectId:form.subjectId||null,teacherId:form.teacherId||null,classroomId:form.classroomId||null,dayOfWeek:Number(form.dayOfWeek)};
     if(editing) await api.put('/timetable/'+editing.id,payload); else await api.post('/timetable',payload);
     setOpen(false);await load();
   }catch(e:any){setError(e?.response?.data?.message||e?.response?.data?.error?.message||'Unable to save timetable entry')}
 }
 async function remove(id:string){
   if(!window.confirm('Delete this timetable entry?')) return;
   try{await api.delete('/timetable/'+id);await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to delete timetable entry')}
 }

 return <Box>
   <Stack direction={{xs:'column',md:'row'}} justifyContent="space-between" alignItems={{md:'center'}} gap={2} sx={{mb:2}}>
     <Box><Typography sx={{fontSize:13,color:'#64748b'}}>Weekly class schedule with batch, teacher and classroom conflict protection.</Typography></Box>
     <Button variant="contained" startIcon={<AddRounded/>} onClick={startCreate} sx={{borderRadius:2,textTransform:'none',fontWeight:800}}>Add timetable slot</Button>
   </Stack>
   {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
   <Card variant="outlined" sx={{borderRadius:3,mb:2}}><CardContent>
    <Grid container spacing={2}>
      <Grid item xs={12} md={6}><FormControl fullWidth size="small"><InputLabel>Batch</InputLabel><Select value={batchId} label="Batch" onChange={e=>setBatchId(e.target.value)}><MenuItem value="">All batches</MenuItem>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select></FormControl></Grid>
      <Grid item xs={12} md={6}><FormControl fullWidth size="small"><InputLabel>Day</InputLabel><Select value={day} label="Day" onChange={e=>setDay(Number(e.target.value))}><MenuItem value={0}>All days</MenuItem>{days.map(d=><MenuItem key={d.v} value={d.v}>{d.n}</MenuItem>)}</Select></FormControl></Grid>
    </Grid>
   </CardContent></Card>
   {!entries.length?<Card variant="outlined" sx={{borderRadius:3}}><CardContent sx={{py:6,textAlign:'center'}}><ScheduleRounded sx={{fontSize:42,color:'#0f766e'}}/><Typography sx={{fontWeight:900,fontSize:18,mt:1}}>No timetable slots yet</Typography><Typography sx={{color:'#64748b',mt:.5}}>Create the first class schedule for this institute.</Typography></CardContent></Card>:
   <Stack gap={2}>{grouped.map(g=><Card key={g.day.v} variant="outlined" sx={{borderRadius:3,overflow:'hidden'}}><Box sx={{px:2,py:1.4,bgcolor:'#f8fafc',borderBottom:'1px solid #e5e7eb',display:'flex',justifyContent:'space-between'}}><Typography sx={{fontWeight:900}}>{g.day.n}</Typography><Chip size="small" label={g.entries.length+' classes'}/></Box><Table size="small"><TableHead><TableRow><TableCell>Time</TableCell><TableCell>Batch</TableCell><TableCell>Subject</TableCell><TableCell>Teacher</TableCell><TableCell>Classroom</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{g.entries.map(e=><TableRow key={e.id} hover><TableCell sx={{fontWeight:800,whiteSpace:'nowrap'}}>{String(e.start_time).slice(0,5)} – {String(e.end_time).slice(0,5)}</TableCell><TableCell>{e.batch_name}</TableCell><TableCell>{e.subject_name}</TableCell><TableCell>{e.teacher_name||'—'}</TableCell><TableCell>{e.classroom_name||'—'}</TableCell><TableCell align="right"><Tooltip title="Edit"><IconButton size="small" onClick={()=>startEdit(e)}><EditRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Delete"><IconButton size="small" onClick={()=>remove(e.id)}><DeleteOutlineRounded fontSize="small"/></IconButton></Tooltip></TableCell></TableRow>)}</TableBody></Table></Card>)}</Stack>}
   <Dialog open={open} onClose={()=>setOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>{editing?'Edit timetable slot':'Add timetable slot'}</DialogTitle><DialogContent>
    <Stack gap={2} sx={{pt:1}}>
      <FormControl fullWidth size="small"><InputLabel>Batch</InputLabel><Select value={form.batchId} label="Batch" onChange={e=>setForm({...form,batchId:e.target.value})}>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select></FormControl>
      <FormControl fullWidth size="small"><InputLabel>Subject</InputLabel><Select value={form.subjectId} label="Subject" onChange={e=>setForm({...form,subjectId:e.target.value})}><MenuItem value="">General / no subject</MenuItem>{subjects.map(s=><MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}</Select></FormControl>
      <Grid container spacing={2}><Grid item xs={12} sm={4}><FormControl fullWidth size="small"><InputLabel>Day</InputLabel><Select value={form.dayOfWeek} label="Day" onChange={e=>setForm({...form,dayOfWeek:Number(e.target.value)})}>{days.map(d=><MenuItem key={d.v} value={d.v}>{d.n}</MenuItem>)}</Select></FormControl></Grid><Grid item xs={6} sm={4}><TextField fullWidth size="small" type="time" label="Start" value={form.startTime} onChange={e=>setForm({...form,startTime:e.target.value})} InputLabelProps={{shrink:true}}/></Grid><Grid item xs={6} sm={4}><TextField fullWidth size="small" type="time" label="End" value={form.endTime} onChange={e=>setForm({...form,endTime:e.target.value})} InputLabelProps={{shrink:true}}/></Grid></Grid>
      <FormControl fullWidth size="small"><InputLabel>Teacher</InputLabel><Select value={form.teacherId} label="Teacher" onChange={e=>setForm({...form,teacherId:e.target.value})}><MenuItem value="">Unassigned</MenuItem>{teachers.map(t=><MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}</Select></FormControl>
      <FormControl fullWidth size="small"><InputLabel>Classroom</InputLabel><Select value={form.classroomId} label="Classroom" onChange={e=>setForm({...form,classroomId:e.target.value})}><MenuItem value="">Unassigned</MenuItem>{classrooms.map(c=><MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}</Select></FormControl>
      <Typography sx={{fontSize:12,color:'#64748b'}}>The backend blocks overlapping batch, teacher and classroom slots.</Typography>
    </Stack>
   </DialogContent><DialogActions><Button onClick={()=>setOpen(false)}>Cancel</Button><Button variant="contained" onClick={save} disabled={!form.batchId}>Save slot</Button></DialogActions></Dialog>
 </Box>
}
