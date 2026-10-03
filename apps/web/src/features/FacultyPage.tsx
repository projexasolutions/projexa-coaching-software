import {useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,FormControl,IconButton,InputLabel,MenuItem,Select,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Tooltip,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,EditRounded,GroupsRounded,MeetingRoomRounded,AssignmentIndRounded} from '@mui/icons-material';
import {api} from '../services';

type Teacher={id:string;employee_code?:string;first_name:string;last_name?:string;email?:string;phone?:string;status:string;batch_count:number;subject_count:number};
type Classroom={id:string;name:string;room_code?:string;capacity?:number;active:boolean};
type Resource={id:string;name:string;code?:string};

function unwrap(r:any){return r?.data?.data??r?.data??[]}
function err(e:any,fallback:string){return e?.response?.data?.message||e?.response?.data?.error?.message||fallback}

export default function FacultyPage(){
 const [teachers,setTeachers]=useState<Teacher[]>([]),[classrooms,setClassrooms]=useState<Classroom[]>([]),[batches,setBatches]=useState<Resource[]>([]),[subjects,setSubjects]=useState<Resource[]>([]);
 const [error,setError]=useState(''),[teacherOpen,setTeacherOpen]=useState(false),[classroomOpen,setClassroomOpen]=useState(false),[assignOpen,setAssignOpen]=useState(false);
 const [editingTeacher,setEditingTeacher]=useState<Teacher|null>(null),[editingClassroom,setEditingClassroom]=useState<Classroom|null>(null),[assignTeacher,setAssignTeacher]=useState<Teacher|null>(null);
 const [teacherForm,setTeacherForm]=useState<any>({employeeCode:'',firstName:'',lastName:'',email:'',phone:'',status:'ACTIVE'});
 const [classroomForm,setClassroomForm]=useState<any>({name:'',roomCode:'',capacity:'',active:true});
 const [batchIds,setBatchIds]=useState<string[]>([]),[subjectIds,setSubjectIds]=useState<string[]>([]);

 async function load(){
   try{
    setError('');
    const [t,c,r]=await Promise.all([api.get('/teachers'),api.get('/classrooms'),api.get('/faculty/resources')]);
    setTeachers(unwrap(t));setClassrooms(unwrap(c));const rr=unwrap(r);setBatches(rr.batches??[]);setSubjects(rr.subjects??[]);
   }catch(e:any){setError(err(e,'Unable to load faculty data'))}
 }
 useEffect(()=>{load()},[]);

 function openTeacher(t?:Teacher){setEditingTeacher(t??null);setTeacherForm(t?{employeeCode:t.employee_code||'',firstName:t.first_name,lastName:t.last_name||'',email:t.email||'',phone:t.phone||'',status:t.status}:{employeeCode:'',firstName:'',lastName:'',email:'',phone:'',status:'ACTIVE'});setTeacherOpen(true)}
 function openClassroom(c?:Classroom){setEditingClassroom(c??null);setClassroomForm(c?{name:c.name,roomCode:c.room_code||'',capacity:c.capacity??'',active:c.active}:{name:'',roomCode:'',capacity:'',active:true});setClassroomOpen(true)}

 async function saveTeacher(){try{const p={...teacherForm,employeeCode:teacherForm.employeeCode||null};if(editingTeacher)await api.put('/teachers/'+editingTeacher.id,p);else await api.post('/teachers',p);setTeacherOpen(false);await load()}catch(e:any){setError(err(e,'Unable to save teacher'))}}
 async function removeTeacher(t:Teacher){if(!window.confirm('Archive this teacher?'))return;try{await api.delete('/teachers/'+t.id);await load()}catch(e:any){setError(err(e,'Unable to archive teacher'))}}
 async function openAssignments(t:Teacher){try{const r=await api.get('/teachers/'+t.id+'/assignments');const x=unwrap(r);setBatchIds((x.batches??[]).map((v:any)=>v.id));setSubjectIds((x.subjects??[]).map((v:any)=>v.id));setAssignTeacher(t);setAssignOpen(true)}catch(e:any){setError(err(e,'Unable to load assignments'))}}
 async function saveAssignments(){if(!assignTeacher)return;try{await api.put('/teachers/'+assignTeacher.id+'/assignments',{batchIds,subjectIds});setAssignOpen(false);await load()}catch(e:any){setError(err(e,'Unable to save assignments'))}}
 async function saveClassroom(){try{const p={...classroomForm,capacity:classroomForm.capacity===''?null:Number(classroomForm.capacity)};if(editingClassroom)await api.put('/classrooms/'+editingClassroom.id,p);else await api.post('/classrooms',p);setClassroomOpen(false);await load()}catch(e:any){setError(err(e,'Unable to save classroom'))}}
 async function removeClassroom(c:Classroom){if(!window.confirm('Archive this classroom?'))return;try{await api.delete('/classrooms/'+c.id);await load()}catch(e:any){setError(err(e,'Unable to archive classroom'))}}

 return <Box>
  <Stack direction={{xs:'column',md:'row'}} spacing={2} sx={{mb:2,justifyContent:'space-between',alignItems:{xs:'stretch',md:'center'}}}>
   <Typography sx={{fontSize:13,color:'#64748b'}}>Manage faculty records, teaching assignments and classroom resources.</Typography>
   <Stack direction="row" spacing={1}><Button variant="outlined" startIcon={<MeetingRoomRounded/>} onClick={()=>openClassroom()} sx={{textTransform:'none',fontWeight:800}}>Add classroom</Button><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>openTeacher()} sx={{textTransform:'none',fontWeight:800}}>Add teacher</Button></Stack>
  </Stack>
  {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'2fr 1fr'},gap:2}}>
   <Card variant="outlined" sx={{borderRadius:3}}><CardContent>
    <Stack direction="row" spacing={1} sx={{mb:1,alignItems:'center'}}><AssignmentIndRounded color="primary"/><Typography sx={{fontWeight:900}}>Faculty</Typography><Chip size="small" label={teachers.length}/></Stack>
    {!teachers.length?<Typography sx={{py:4,textAlign:'center',color:'#64748b'}}>No teachers configured yet.</Typography>:<Table size="small"><TableHead><TableRow><TableCell>Teacher</TableCell><TableCell>Status</TableCell><TableCell>Assignments</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{teachers.map(t=><TableRow key={t.id} hover><TableCell><Typography sx={{fontWeight:800}}>{t.first_name} {t.last_name||''}</Typography><Typography sx={{fontSize:11,color:'#64748b'}}>{t.employee_code||t.email||'No employee code'}</Typography></TableCell><TableCell><Chip size="small" label={t.status}/></TableCell><TableCell>{t.batch_count} batches · {t.subject_count} subjects</TableCell><TableCell align="right"><Tooltip title="Assignments"><IconButton size="small" onClick={()=>openAssignments(t)}><GroupsRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Edit"><IconButton size="small" onClick={()=>openTeacher(t)}><EditRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Archive"><IconButton size="small" onClick={()=>removeTeacher(t)}><DeleteOutlineRounded fontSize="small"/></IconButton></Tooltip></TableCell></TableRow>)}</TableBody></Table>}
   </CardContent></Card>
   <Card variant="outlined" sx={{borderRadius:3}}><CardContent>
    <Stack direction="row" spacing={1} sx={{mb:1,alignItems:'center'}}><MeetingRoomRounded color="primary"/><Typography sx={{fontWeight:900}}>Classrooms</Typography><Chip size="small" label={classrooms.length}/></Stack>
    <Stack spacing={1}>{classrooms.map(c=><Box key={c.id} sx={{p:1.2,border:'1px solid #e5e7eb',borderRadius:2,display:'flex',alignItems:'center',gap:1}}><Box sx={{flex:1}}><Typography sx={{fontWeight:800,fontSize:13}}>{c.name}</Typography><Typography sx={{fontSize:11,color:'#64748b'}}>{c.room_code||'No room code'} · {c.capacity??'No capacity'}</Typography></Box><IconButton size="small" onClick={()=>openClassroom(c)}><EditRounded fontSize="small"/></IconButton><IconButton size="small" onClick={()=>removeClassroom(c)}><DeleteOutlineRounded fontSize="small"/></IconButton></Box>)}{!classrooms.length&&<Typography sx={{py:3,textAlign:'center',color:'#64748b'}}>No classrooms configured.</Typography>}</Stack>
   </CardContent></Card>
  </Box>

  <Dialog open={teacherOpen} onClose={()=>setTeacherOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>{editingTeacher?'Edit teacher':'Add teacher'}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'1fr 1fr'},gap:2}}><TextField size="small" label="First name" value={teacherForm.firstName} onChange={e=>setTeacherForm({...teacherForm,firstName:e.target.value})}/><TextField size="small" label="Last name" value={teacherForm.lastName} onChange={e=>setTeacherForm({...teacherForm,lastName:e.target.value})}/><TextField size="small" label="Employee code" value={teacherForm.employeeCode} onChange={e=>setTeacherForm({...teacherForm,employeeCode:e.target.value})}/><FormControl size="small"><InputLabel>Status</InputLabel><Select label="Status" value={teacherForm.status} onChange={e=>setTeacherForm({...teacherForm,status:e.target.value})}><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="ON_LEAVE">On leave</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem><MenuItem value="ARCHIVED">Archived</MenuItem></Select></FormControl></Box>
   <TextField size="small" label="Email" value={teacherForm.email} onChange={e=>setTeacherForm({...teacherForm,email:e.target.value})}/><TextField size="small" label="Phone" value={teacherForm.phone} onChange={e=>setTeacherForm({...teacherForm,phone:e.target.value})}/>
  </Stack></DialogContent><DialogActions><Button onClick={()=>setTeacherOpen(false)}>Cancel</Button><Button variant="contained" disabled={!teacherForm.firstName.trim()} onClick={saveTeacher}>Save teacher</Button></DialogActions></Dialog>

  <Dialog open={assignOpen} onClose={()=>setAssignOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>Teaching assignments · {assignTeacher?.first_name}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <FormControl fullWidth size="small"><InputLabel>Batches</InputLabel><Select multiple label="Batches" value={batchIds} onChange={e=>setBatchIds(e.target.value as string[])}>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select></FormControl>
   <FormControl fullWidth size="small"><InputLabel>Subjects</InputLabel><Select multiple label="Subjects" value={subjectIds} onChange={e=>setSubjectIds(e.target.value as string[])}>{subjects.map(s=><MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}</Select></FormControl>
   <Typography sx={{fontSize:12,color:'#64748b'}}>These assignments are used by timetable and attendance validation.</Typography>
  </Stack></DialogContent><DialogActions><Button onClick={()=>setAssignOpen(false)}>Cancel</Button><Button variant="contained" onClick={saveAssignments}>Save assignments</Button></DialogActions></Dialog>

  <Dialog open={classroomOpen} onClose={()=>setClassroomOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>{editingClassroom?'Edit classroom':'Add classroom'}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <TextField size="small" label="Name" value={classroomForm.name} onChange={e=>setClassroomForm({...classroomForm,name:e.target.value})}/><Box sx={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:2}}><TextField size="small" label="Room code" value={classroomForm.roomCode} onChange={e=>setClassroomForm({...classroomForm,roomCode:e.target.value})}/><TextField size="small" type="number" label="Capacity" value={classroomForm.capacity} onChange={e=>setClassroomForm({...classroomForm,capacity:e.target.value})}/></Box>
   <FormControl size="small"><InputLabel>Status</InputLabel><Select label="Status" value={classroomForm.active?'ACTIVE':'INACTIVE'} onChange={e=>setClassroomForm({...classroomForm,active:e.target.value==='ACTIVE'})}><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem></Select></FormControl>
  </Stack></DialogContent><DialogActions><Button onClick={()=>setClassroomOpen(false)}>Cancel</Button><Button variant="contained" disabled={!classroomForm.name.trim()} onClick={saveClassroom}>Save classroom</Button></DialogActions></Dialog>
 </Box>
}
