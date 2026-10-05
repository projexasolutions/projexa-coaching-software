import {useEffect,useMemo,useState} from 'react';
import {api} from '../services';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Divider,MenuItem,Select,Stack,Tab,Tabs,TextField,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,RefreshRounded,SchoolRounded,SubjectRounded,MeetingRoomRounded,LayersRounded} from '@mui/icons-material';
import ProgramConfigurationPage from './ProgramConfigurationPage';

const unwrap=(r:any)=>r?.data?.data??r?.data;
const message=(e:any)=>e?.response?.data?.message??e?.response?.data?.error?.message??e?.message??'Request failed';

function useLive(endpoint:string){
 const [rows,setRows]=useState<any>([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
 const load=async()=>{setLoading(true);setError('');try{const d=unwrap(await api.get(endpoint));setRows(Array.isArray(d)?d:[])}catch(e){setError(message(e))}finally{setLoading(false)}};
 useEffect(()=>{load()},[endpoint]); return {rows,loading,error,load};
}
function Metric({label,value,icon:Icon}:{label:string;value:any;icon:any}){return <Card sx={{height:'100%'}}><CardContent><Stack direction="row" spacing={1.5} alignItems="center"><Box sx={{width:40,height:40,borderRadius:2.5,bgcolor:'#e8f8f5',color:'#0f766e',display:'grid',placeItems:'center'}}><Icon fontSize="small"/></Box><Box><Typography sx={{fontSize:11,color:'#64748b'}}>{label}</Typography><Typography sx={{fontSize:25,fontWeight:950,lineHeight:1.1}}>{value}</Typography></Box></Stack></CardContent></Card>}

export default function AcademicsWorkspacePage(){
 const [tab,setTab]=useState(0);
 const overview=useLive('/academic-configuration/overview');
 const years=useLive('/academic-years'), classes=useLive('/classes'), streams=useLive('/streams'), subjects=useLive('/subjects'), batches=useLive('/batches'), rooms=useLive('/academic-configuration/classrooms'), classStreams=useLive('/academic-configuration/class-streams'), classSubjects=useLive('/academic-configuration/class-subjects'), programs=useLive('/programs');
 const reloadAll=()=>[overview,years,classes,streams,subjects,batches,rooms,classStreams,classSubjects,programs].forEach(x=>x.load());
 const tabs=['Structure','Classes & Streams','Subjects','Batches','Classrooms','Programs & Exams'];
 return <Stack spacing={2.5}>
  <Stack direction={{xs:'column',md:'row'}} justifyContent="space-between" gap={1}>
   <Box><Typography sx={{fontSize:12,color:'#64748b',fontWeight:800}}>ACADEMIC CONFIGURATION</Typography><Typography sx={{fontSize:13,color:'#94a3b8',mt:.3}}>Build the institute structure once; everything else uses it.</Typography></Box>
   <Button variant="outlined" startIcon={<RefreshRounded/>} onClick={reloadAll}>Refresh all</Button>
  </Stack>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'repeat(2,1fr)',md:'repeat(4,1fr)',xl:'repeat(8,1fr)'},gap:1.25}}>
   {[['Years',overview.rows.academicYears,SchoolRounded],['Classes',overview.rows.classes,LayersRounded],['Streams',overview.rows.streams,LayersRounded],['Subjects',overview.rows.subjects,SubjectRounded],['Batches',overview.rows.batches,LayersRounded],['Rooms',overview.rows.classrooms,MeetingRoomRounded],['Class→Streams',overview.rows.classStreams,LayersRounded],['Class→Subjects',overview.rows.classSubjects,SubjectRounded]].map(([l,v,I])=><Metric key={String(l)} label={String(l)} value={v??0} icon={I}/>)}
  </Box>
  <Card><CardContent sx={{p:{xs:1,md:1.5}}}><Tabs value={tab} onChange={(_,v)=>setTab(v)} variant="scrollable" scrollButtons="auto">{tabs.map(x=><Tab key={x} label={x}/>)}</Tabs></CardContent></Card>
  {tab===0&&<StructureTab years={years} classes={classes} streams={streams} subjects={subjects}/>}
  {tab===1&&<MappingTab type="stream" years={years.rows} classes={classes.rows} streams={streams.rows} data={classStreams.rows} reload={classStreams.load}/>}
  {tab===2&&<MappingTab type="subject" years={years.rows} classes={classes.rows} streams={subjects.rows} data={classSubjects.rows} reload={classSubjects.load}/>}
  {tab===3&&<BatchesTab batches={batches} years={years.rows} classes={classes.rows} streams={streams.rows} programs={programs.rows}/>}
  {tab===4&&<RoomsTab rooms={rooms}/>}
  {tab===5&&<ProgramConfigurationPage/>}
 </Stack>
}

function StructureTab({years,classes,streams,subjects}:any){
 return <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'1fr 1fr'},gap:2}}>
  <ResourceCard title="Academic years" endpoint="/academic-years" rows={years.rows} fields={['name','startDate','endDate','current','status']} load={years.load}/>
  <ResourceCard title="Classes" endpoint="/classes" rows={classes.rows} fields={['name','displayOrder','active']} load={classes.load}/>
  <ResourceCard title="Streams" endpoint="/streams" rows={streams.rows} fields={['name','code','active']} load={streams.load}/>
  <ResourceCard title="Subjects" endpoint="/subjects" rows={subjects.rows} fields={['name','code','active']} load={subjects.load}/>
 </Box>
}

const labelMap:any={name:'Name',startDate:'Start date',endDate:'End date',current:'Current',status:'Status',displayOrder:'Order',active:'Active',code:'Code'};
function ResourceCard({title,endpoint,rows,fields,load}:any){
 const [open,setOpen]=useState(false),[edit,setEdit]=useState<any>(null);
 const val=(r:any,f:string)=>{const v=r[f]??(f==='current'?r.isCurrent:undefined);return typeof v==='boolean'?(v?'Yes':'No'):(v??'—')};
 return <Card><CardContent sx={{p:0}}>
  <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{p:2}}><Box><Typography fontWeight={900}>{title}</Typography><Typography fontSize={11} color="text.secondary">{rows.length} configured</Typography></Box><Button size="small" variant="contained" startIcon={<AddRounded/>} onClick={()=>{setEdit(null);setOpen(true)}}>Add</Button></Stack><Divider/>
  <Box sx={{overflowX:'auto'}}><table style={{width:'100%',borderCollapse:'collapse'}}><thead><tr>{fields.map((f:string)=><th key={f} style={{padding:'10px 14px',textAlign:'left',fontSize:10,color:'#64748b'}}>{labelMap[f]||f}</th>)}<th/></tr></thead><tbody>{rows.map((r:any)=><tr key={r.id}>{fields.map((f:string)=><td key={f} style={{padding:'10px 14px',fontSize:12,borderTop:'1px solid #f1f5f9'}}>{val(r,f)}</td>)}<td style={{padding:8,textAlign:'right',borderTop:'1px solid #f1f5f9'}}><Stack direction="row" spacing={0.5} justifyContent="flex-end"><Button size="small" onClick={()=>{setEdit(r);setOpen(true)}}>Edit</Button><Button size="small" color="error" onClick={async()=>{if(confirm('Delete this record?')){try{await api.delete(endpoint+'/'+r.id);load()}catch(e){alert(message(e))}}}}>Delete</Button></Stack></td></tr>)}{!rows.length&&<tr><td colSpan={fields.length+1} style={{padding:28,textAlign:'center',color:'#94a3b8',fontSize:12}}>No records yet.</td></tr>}</tbody></table></Box>
  <AcademicDialog open={open} initial={edit} endpoint={endpoint} fields={fields} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);load()}}/>
 </CardContent></Card>
}

function AcademicDialog({open,initial,endpoint,fields,onClose,onSaved}:any){
 const [f,setF]=useState<any>({}),[saving,setSaving]=useState(false),[error,setError]=useState('');
 useEffect(()=>{const x:any={};fields.forEach((k:string)=>x[k]=initial?.[k]??(k==='active'||k==='current'?true:k==='status'?'ACTIVE':''));setF(x);setError('')},[open,initial,fields.join(',')]);
 const set=(k:string)=>(e:any)=>setF((x:any)=>({...x,[k]:e.target.value}));
 const save=async()=>{setSaving(true);setError('');try{const p={...f};if('displayOrder' in p)p.displayOrder=Number(p.displayOrder||0);await (initial?api.put(endpoint+'/'+initial.id,p):api.post(endpoint,p));onSaved()}catch(e){setError(message(e))}finally{setSaving(false)}};
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>{initial?'Edit':'Add'} configuration</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}{fields.map((k:string)=>k==='active'||k==='current'?<Select key={k} value={String(f[k])} onChange={e=>setF((x:any)=>({...x,[k]:e.target.value==='true'}))}>{<MenuItem value="true">{k==='current'?'Current':'Active'}</MenuItem>}<MenuItem value="false">{k==='current'?'Not current':'Inactive'}</MenuItem></Select>:k==='status'?<Select key={k} value={f[k]||'ACTIVE'} onChange={set(k)}><MenuItem value="ACTIVE">ACTIVE</MenuItem><MenuItem value="ARCHIVED">ARCHIVED</MenuItem></Select>:<TextField key={k} label={labelMap[k]||k} type={k.includes('Date')?'date':k==='displayOrder'?'number':'text'} value={f[k]??''} onChange={set(k)} slotProps={k.includes('Date')?{inputLabel:{shrink:true}}:undefined}/>)}</Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" disabled={saving} onClick={save}>Save</Button></DialogActions></Dialog>
}

function MappingTab({type,years,classes,streams,data,reload}:any){
 const [open,setOpen]=useState(false);
 const title=type==='stream'?'Class → Streams':'Class → Subjects'; const endpoint=type==='stream'?'/academic-configuration/class-streams':'/academic-configuration/class-subjects';
 const remove=async(id:string)=>{if(confirm('Remove this mapping?')){try{await api.delete(endpoint+'/'+id);reload()}catch(e){alert(message(e))}}};
 return <Card><CardContent><Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" gap={1} mb={1.5}><Box><Typography fontWeight={900}>{title}</Typography><Typography fontSize={11} color="text.secondary">Control which streams/subjects belong to each class in each academic year.</Typography></Box><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>setOpen(true)}>Assign</Button></Stack><Box sx={{overflowX:'auto'}}><table style={{width:'100%',borderCollapse:'collapse'}}><thead><tr>{['Academic year','Class',type==='stream'?'Stream':'Subject',''].map(x=><th key={x} style={{padding:'10px 14px',textAlign:'left',fontSize:10,color:'#64748b'}}>{x}</th>)}</tr></thead><tbody>{data.map((r:any)=><tr key={r.id}>{[type==='stream'?r.academic_year_name:r.academic_year_name,type==='stream'?r.class_name:r.class_name,type==='stream'?r.stream_name:r.subject_name].map((x:any,i:number)=><td key={i} style={{padding:'11px 14px',fontSize:12,borderTop:'1px solid #f1f5f9'}}>{x}</td>)}<td style={{padding:8,textAlign:'right',borderTop:'1px solid #f1f5f9'}}><Button size="small" color="error" onClick={()=>remove(r.id)}>Remove</Button></td></tr>)}{!data.length&&<tr><td colSpan={4} style={{padding:32,textAlign:'center',color:'#94a3b8'}}>No mappings configured.</td></tr>}</tbody></table></Box></CardContent><MappingDialog open={open} type={type} years={years} classes={classes} streams={streams} endpoint={endpoint} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);reload()}}/></Card>
}

function MappingDialog({open,type,years,classes,streams,endpoint,onClose,onSaved}:any){
 const [f,setF]=useState<any>({academicYearId:'',classId:'',streamId:'',subjectId:''}),[error,setError]=useState('');
 useEffect(()=>{setF({academicYearId:years.find((x:any)=>x.isCurrent)?.id||years[0]?.id||'',classId:'',streamId:'',subjectId:''});setError('')},[open,years]);
 const save=async()=>{const key=type==='stream'?'streamId':'subjectId';if(!f.academicYearId||!f.classId||!f[key]){setError('Academic year, class and selection are required.');return}try{await api.post(endpoint,f);onSaved()}catch(e){setError(message(e))}};
 const options=type==='stream'?streams:streams;
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>Assign {type==='stream'?'stream':'subject'}</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}<Select value={f.academicYearId} onChange={e=>setF((x:any)=>({...x,academicYearId:e.target.value}))}>{years.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}{x.isCurrent?' • Current':''}</MenuItem>)}</Select><Select value={f.classId} displayEmpty onChange={e=>setF((x:any)=>({...x,classId:e.target.value}))}><MenuItem value="">Select class</MenuItem>{classes.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select><Select value={f[type==='stream'?'streamId':'subjectId']} displayEmpty onChange={e=>setF((x:any)=>({...x,[type==='stream'?'streamId':'subjectId']:e.target.value}))}><MenuItem value="">Select {type}</MenuItem>{options.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save}>Assign</Button></DialogActions></Dialog>
}

function BatchesTab({batches,years,classes,streams,programs}:any){
 const [open,setOpen]=useState(false),[edit,setEdit]=useState<any>(null);
 return <Card><CardContent><Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}><Box><Typography fontWeight={900}>Batches</Typography><Typography fontSize={11} color="text.secondary">Operational groups students are enrolled into.</Typography></Box><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>{setEdit(null);setOpen(true)}}>Add batch</Button></Stack><Box sx={{overflowX:'auto'}}><table style={{width:'100%',borderCollapse:'collapse'}}><thead><tr>{['Batch','Class','Stream','Academic year','Capacity','Status',''].map(x=><th key={x} style={{padding:'10px 14px',textAlign:'left',fontSize:10,color:'#64748b'}}>{x}</th>)}</tr></thead><tbody>{batches.rows.map((r:any)=><tr key={r.id}>{[r.name,classes.find((x:any)=>x.id===r.classId)?.name||r.class_name||'—',streams.find((x:any)=>x.id===r.streamId)?.name||r.stream_name||'—',years.find((x:any)=>x.id===r.academicYearId)?.name||r.academic_year_name||'—',r.capacity??'—',r.status].map((x:any,i:number)=><td key={i} style={{padding:'11px 14px',fontSize:12,borderTop:'1px solid #f1f5f9'}}>{i===5?<Chip size="small" label={x} variant="outlined"/>:x}</td>)}<td style={{padding:8,textAlign:'right',borderTop:'1px solid #f1f5f9'}}><Stack direction="row" spacing={0.5} justifyContent="flex-end"><Button size="small" onClick={()=>{setEdit(r);setOpen(true)}}>Edit</Button><Button size="small" color="error" onClick={async()=>{if(confirm('Delete this batch?')){try{await api.delete('/batches/'+r.id);batches.load()}catch(e){alert(message(e))}}}}>Delete</Button></Stack></td></tr>)}{!batches.rows.length&&<tr><td colSpan={7} style={{padding:32,textAlign:'center',color:'#94a3b8'}}>No batches configured.</td></tr>}</tbody></table></Box></CardContent><BatchDialog open={open} initial={edit} years={years} classes={classes} streams={streams} programs={programs} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);batches.load()}}/></Card>
}

function BatchDialog({open,initial,years,classes,streams,programs,onClose,onSaved}:any){
 const [f,setF]=useState<any>({academicYearId:'',classId:'',streamId:'',name:'',code:'',capacity:'',status:'ACTIVE'}),[error,setError]=useState('');
 useEffect(()=>{setF({academicYearId:initial?.academicYearId||initial?.academic_year_id||years.find((x:any)=>x.isCurrent)?.id||years[0]?.id||'',classId:initial?.classId||initial?.class_id||'',streamId:initial?.streamId||initial?.stream_id||'',programId:initial?.programId||initial?.program_id||'',name:initial?.name||'',code:initial?.code||'',capacity:initial?.capacity??'',status:initial?.status||'ACTIVE'});setError('')},[open,initial,years]);
 const set=(k:string)=>(e:any)=>setF((x:any)=>({...x,[k]:e.target.value}));
 const save=async()=>{if(!f.academicYearId||!f.classId||!f.name.trim()){setError('Academic year, class and batch name are required.');return}try{await (initial?api.put('/batches/'+initial.id,{...f,capacity:f.capacity===''?null:Number(f.capacity),streamId:f.streamId||null,programId:f.programId||null}):api.post('/batches',{...f,capacity:f.capacity===''?null:Number(f.capacity),streamId:f.streamId||null,programId:f.programId||null}));onSaved()}catch(e){setError(message(e))}};
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>{initial?'Edit':'Add'} batch</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}<Select value={f.academicYearId} onChange={set('academicYearId')}>{years.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select><Select value={f.classId} displayEmpty onChange={set('classId')}><MenuItem value="">Select class</MenuItem>{classes.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select><Select value={f.streamId} displayEmpty onChange={set('streamId')}><MenuItem value="">No stream</MenuItem>{streams.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select><Select value={f.programId} displayEmpty onChange={set('programId')}><MenuItem value="">No program</MenuItem>{programs.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select><TextField label="Batch name" value={f.name} onChange={set('name')}/><TextField label="Code" value={f.code} onChange={set('code')}/><TextField label="Capacity" type="number" value={f.capacity} onChange={set('capacity')}/><Select value={f.status} onChange={set('status')}><MenuItem value="ACTIVE">ACTIVE</MenuItem><MenuItem value="INACTIVE">INACTIVE</MenuItem></Select></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save}>Save batch</Button></DialogActions></Dialog>
}

function RoomsTab({rooms}:any){
 const [open,setOpen]=useState(false),[edit,setEdit]=useState<any>(null);
 return <Card><CardContent><Stack direction="row" justifyContent="space-between" mb={1.5}><Box><Typography fontWeight={900}>Classrooms</Typography><Typography fontSize={11} color="text.secondary">Rooms available for timetable scheduling.</Typography></Box><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>{setEdit(null);setOpen(true)}}>Add classroom</Button></Stack><Box sx={{overflowX:'auto'}}><table style={{width:'100%',borderCollapse:'collapse'}}><thead><tr>{['Name','Room code','Capacity','Status',''].map(x=><th key={x} style={{padding:'10px 14px',textAlign:'left',fontSize:10,color:'#64748b'}}>{x}</th>)}</tr></thead><tbody>{rooms.rows.map((r:any)=><tr key={r.id}>{[r.name,r.room_code,r.capacity??'—',r.active?'ACTIVE':'INACTIVE'].map((x:any,i:number)=><td key={i} style={{padding:'11px 14px',fontSize:12,borderTop:'1px solid #f1f5f9'}}>{i===3?<Chip size="small" label={x} variant="outlined"/>:x}</td>)}<td style={{padding:8,textAlign:'right',borderTop:'1px solid #f1f5f9'}}><Stack direction="row" spacing={0.5} justifyContent="flex-end"><Button size="small" onClick={()=>{setEdit(r);setOpen(true)}}>Edit</Button><Button size="small" color="error" onClick={async()=>{if(confirm('Delete this classroom?')){try{await api.delete('/academic-configuration/classrooms/'+r.id);rooms.load()}catch(e){alert(message(e))}}}}>Delete</Button></Stack></td></tr>)}{!rooms.rows.length&&<tr><td colSpan={5} style={{padding:32,textAlign:'center',color:'#94a3b8'}}>No classrooms configured.</td></tr>}</tbody></table></Box></CardContent><RoomDialog open={open} initial={edit} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);rooms.load()}}/></Card>
}
function RoomDialog({open,initial,onClose,onSaved}:any){
 const [f,setF]=useState<any>({name:'',roomCode:'',capacity:'',active:true}),[error,setError]=useState('');
 useEffect(()=>{setF({name:initial?.name||'',roomCode:initial?.room_code||'',capacity:initial?.capacity??'',active:initial?.active??true});setError('')},[open,initial]);
 const save=async()=>{if(!f.name.trim()){setError('Classroom name is required.');return}try{const p={...f,capacity:f.capacity===''?null:Number(f.capacity)};if(initial)await api.put('/academic-configuration/classrooms/'+initial.id,p);else await api.post('/academic-configuration/classrooms',p);onSaved()}catch(e){setError(message(e))}};
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>{initial?'Edit':'Add'} classroom</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}<TextField label="Classroom name" value={f.name} onChange={e=>setF((x:any)=>({...x,name:e.target.value}))}/><TextField label="Room code" value={f.roomCode} onChange={e=>setF((x:any)=>({...x,roomCode:e.target.value}))}/><TextField label="Capacity" type="number" value={f.capacity} onChange={e=>setF((x:any)=>({...x,capacity:e.target.value}))}/><Select value={String(f.active)} onChange={e=>setF((x:any)=>({...x,active:e.target.value==='true'}))}><MenuItem value="true">ACTIVE</MenuItem><MenuItem value="false">INACTIVE</MenuItem></Select></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save}>Save classroom</Button></DialogActions></Dialog>
}
