import {useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,FormControl,IconButton,InputLabel,MenuItem,Select,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Tooltip,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,EditRounded,AssignmentRounded,GradingRounded} from '@mui/icons-material';
import {api} from '../services';

type Homework={id:string;batch_id:string;teacher_id?:string;title:string;description?:string;due_at?:string;batch_name:string;teacher_name?:string;submission_count:number};
type Resource={id:string;name:string;first_name?:string;last_name?:string};

function unwrap(r:any){return r?.data?.data??r?.data??[]}
function err(e:any,fallback:string){return e?.response?.data?.message||e?.response?.data?.error?.message||fallback}

export default function HomeworkPage(){
 const [items,setItems]=useState<Homework[]>([]),[batches,setBatches]=useState<Resource[]>([]),[teachers,setTeachers]=useState<Resource[]>([]);
 const [batchId,setBatchId]=useState(''),[error,setError]=useState(''),[open,setOpen]=useState(false),[subOpen,setSubOpen]=useState(false),[editing,setEditing]=useState<Homework|null>(null),[selected,setSelected]=useState<Homework|null>(null),[subs,setSubs]=useState<any[]>([]);
 const [form,setForm]=useState<any>({batchId:'',teacherId:'',title:'',description:'',dueAt:''});

 async function load(){
  try{setError('');const [h,r]=await Promise.all([api.get('/homework',{params:{batchId:batchId||undefined}}),api.get('/homework/resources')]);setItems(unwrap(h));const x=unwrap(r);setBatches(x.batches??[]);setTeachers(x.teachers??[])}
  catch(e:any){setError(err(e,'Unable to load homework'))}
 }
 useEffect(()=>{load()},[batchId]);

 function edit(x?:Homework){setEditing(x??null);setForm(x?{batchId:x.batch_id,teacherId:x.teacher_id||'',title:x.title,description:x.description||'',dueAt:x.due_at?String(x.due_at).slice(0,16):''}:{batchId:batchId||batches[0]?.id||'',teacherId:'',title:'',description:'',dueAt:''});setOpen(true)}
 async function save(){try{const p={...form,teacherId:form.teacherId||null,dueAt:form.dueAt||null};if(editing)await api.put('/homework/'+editing.id,p);else await api.post('/homework',p);setOpen(false);await load()}catch(e:any){setError(err(e,'Unable to save homework'))}}
 async function remove(x:Homework){if(!window.confirm('Delete this homework?'))return;try{await api.delete('/homework/'+x.id);await load()}catch(e:any){setError(err(e,'Unable to delete homework'))}}
 async function submissions(x:Homework){try{setSelected(x);const r=await api.get('/homework/'+x.id+'/submissions');setSubs(unwrap(r));setSubOpen(true)}catch(e:any){setError(err(e,'Unable to load submissions'))}}

 return <Box>
  <Stack direction={{xs:'column',md:'row'}} spacing={2} sx={{mb:2,justifyContent:'space-between',alignItems:{xs:'stretch',md:'center'}}}>
   <Typography sx={{fontSize:13,color:'#64748b'}}>Create homework, assign it to a batch and track student submissions and grading.</Typography>
   <Button variant="contained" startIcon={<AddRounded/>} onClick={()=>edit()} sx={{textTransform:'none',fontWeight:800}}>Create homework</Button>
  </Stack>
  {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
  <Card variant="outlined" sx={{borderRadius:3,mb:2}}><CardContent><FormControl size="small" sx={{minWidth:{xs:'100%',sm:280}}}><InputLabel>Batch</InputLabel><Select label="Batch" value={batchId} onChange={e=>setBatchId(e.target.value)}><MenuItem value="">All batches</MenuItem>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select></FormControl></CardContent></Card>
  {!items.length?<Card variant="outlined" sx={{borderRadius:3}}><CardContent sx={{py:6,textAlign:'center'}}><AssignmentRounded sx={{fontSize:44,color:'#0f766e'}}/><Typography sx={{fontWeight:900,fontSize:18,mt:1}}>No homework yet</Typography><Typography sx={{color:'#64748b',mt:.5}}>Create the first assignment for a batch.</Typography></CardContent></Card>:
  <Card variant="outlined" sx={{borderRadius:3}}><Table size="small"><TableHead><TableRow><TableCell>Homework</TableCell><TableCell>Batch</TableCell><TableCell>Teacher</TableCell><TableCell>Due</TableCell><TableCell>Submissions</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{items.map(x=><TableRow key={x.id} hover><TableCell><Typography sx={{fontWeight:800}}>{x.title}</Typography><Typography sx={{fontSize:11,color:'#64748b'}}>{x.description||'No description'}</Typography></TableCell><TableCell>{x.batch_name}</TableCell><TableCell>{x.teacher_name||'Unassigned'}</TableCell><TableCell>{x.due_at?new Date(x.due_at).toLocaleString():'No deadline'}</TableCell><TableCell><Chip size="small" label={x.submission_count}/></TableCell><TableCell align="right"><Tooltip title="Submissions"><IconButton size="small" onClick={()=>submissions(x)}><GradingRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Edit"><IconButton size="small" onClick={()=>edit(x)}><EditRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Delete"><IconButton size="small" onClick={()=>remove(x)}><DeleteOutlineRounded fontSize="small"/></IconButton></Tooltip></TableCell></TableRow>)}</TableBody></Table></Card>}
  <Dialog open={open} onClose={()=>setOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>{editing?'Edit homework':'Create homework'}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <FormControl fullWidth size="small"><InputLabel>Batch</InputLabel><Select label="Batch" value={form.batchId} onChange={e=>setForm({...form,batchId:e.target.value})}>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select></FormControl>
   <FormControl fullWidth size="small"><InputLabel>Teacher</InputLabel><Select label="Teacher" value={form.teacherId} onChange={e=>setForm({...form,teacherId:e.target.value})}><MenuItem value="">Unassigned</MenuItem>{teachers.map(t=><MenuItem key={t.id} value={t.id}>{t.first_name} {t.last_name||''}</MenuItem>)}</Select></FormControl>
   <TextField size="small" fullWidth label="Title" value={form.title} onChange={e=>setForm({...form,title:e.target.value})}/><TextField size="small" fullWidth multiline minRows={3} label="Description" value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/><TextField size="small" fullWidth type="datetime-local" label="Due date" value={form.dueAt} onChange={e=>setForm({...form,dueAt:e.target.value})} slotProps={{inputLabel:{shrink:true}}}/>
  </Stack></DialogContent><DialogActions><Button onClick={()=>setOpen(false)}>Cancel</Button><Button variant="contained" disabled={!form.batchId||!form.title.trim()} onClick={save}>Save homework</Button></DialogActions></Dialog>
  <Dialog open={subOpen} onClose={()=>setSubOpen(false)} fullWidth maxWidth="md"><DialogTitle sx={{fontWeight:900}}>Submissions · {selected?.title}</DialogTitle><DialogContent sx={{px:0}}>{!subs.length?<Typography sx={{p:4,textAlign:'center',color:'#64748b'}}>No active students found.</Typography>:<Table size="small"><TableHead><TableRow><TableCell>Student</TableCell><TableCell>Status</TableCell><TableCell>Score</TableCell><TableCell>Submitted</TableCell></TableRow></TableHead><TableBody>{subs.map(s=><TableRow key={s.student_id}><TableCell>{s.first_name} {s.last_name||''}<Typography sx={{fontSize:11,color:'#64748b'}}>{s.admission_number}</Typography></TableCell><TableCell><Chip size="small" label={s.status||'PENDING'}/></TableCell><TableCell>{s.score??'—'}</TableCell><TableCell>{s.submitted_at?new Date(s.submitted_at).toLocaleString():'Not submitted'}</TableCell></TableRow>)}</TableBody></Table>}</DialogContent><DialogActions><Button onClick={()=>setSubOpen(false)}>Close</Button></DialogActions></Dialog>
 </Box>
}
