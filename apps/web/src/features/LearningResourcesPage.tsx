import {useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,FormControl,IconButton,InputLabel,MenuItem,Select,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Tooltip,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,EditRounded,MenuBookRounded,OpenInNewRounded} from '@mui/icons-material';
import {api} from '../services';

type Resource={id:string;batch_id?:string;subject_id?:string;teacher_id?:string;title:string;description?:string;resource_type:string;resource_url:string;topic?:string;published:boolean;batch_name?:string;subject_name?:string;teacher_name?:string};
type Ref={id:string;name:string;code?:string};
const types=['LINK','PDF','VIDEO','DOCUMENT','RECORDING'];
function unwrap(r:any){return r?.data?.data??r?.data??[]}
function err(e:any,fallback:string){return e?.response?.data?.message||e?.response?.data?.error?.message||fallback}

export default function LearningResourcesPage(){
 const [items,setItems]=useState<Resource[]>([]),[batches,setBatches]=useState<Ref[]>([]),[subjects,setSubjects]=useState<Ref[]>([]),[teachers,setTeachers]=useState<Ref[]>([]);
 const [batchId,setBatchId]=useState(''),[subjectId,setSubjectId]=useState(''),[error,setError]=useState('');
 const [open,setOpen]=useState(false),[editing,setEditing]=useState<Resource|null>(null);
 const [form,setForm]=useState<any>({batchId:'',subjectId:'',teacherId:'',title:'',description:'',resourceType:'LINK',resourceUrl:'',topic:'',published:false});
 async function load(){try{setError('');const [r,ref]=await Promise.all([api.get('/learning/resources',{params:{batchId:batchId||undefined,subjectId:subjectId||undefined}}),api.get('/learning/resources/resources')]);setItems(unwrap(r));const x=unwrap(ref);setBatches(x.batches??[]);setSubjects(x.subjects??[]);setTeachers(x.teachers??[])}catch(e:any){setError(err(e,'Unable to load learning resources'))}}
 useEffect(()=>{load()},[batchId,subjectId]);
 function openCreate(){setEditing(null);setForm({batchId:batchId||'',subjectId:subjectId||'',teacherId:'',title:'',description:'',resourceType:'LINK',resourceUrl:'',topic:'',published:false});setOpen(true)}
 function openEdit(x:Resource){setEditing(x);setForm({batchId:x.batch_id||'',subjectId:x.subject_id||'',teacherId:x.teacher_id||'',title:x.title,description:x.description||'',resourceType:x.resource_type,resourceUrl:x.resource_url,topic:x.topic||'',published:x.published});setOpen(true)}
 async function save(){try{const p={...form,batchId:form.batchId||null,subjectId:form.subjectId||null,teacherId:form.teacherId||null};if(editing)await api.put('/learning/resources/'+editing.id,p);else await api.post('/learning/resources',p);setOpen(false);await load()}catch(e:any){setError(err(e,'Unable to save learning resource'))}}
 async function remove(id:string){if(!window.confirm('Delete this learning resource?'))return;try{await api.delete('/learning/resources/'+id);await load()}catch(e:any){setError(err(e,'Unable to delete learning resource'))}}
 return <Box>
  <Stack direction={{xs:'column',md:'row'}} spacing={2} sx={{mb:2,justifyContent:'space-between',alignItems:{xs:'stretch',md:'center'}}}>
   <Box><Typography sx={{fontSize:13,color:'#64748b'}}>Publish notes, PDFs, videos and recordings to the right batch and subject.</Typography></Box>
   <Button variant="contained" startIcon={<AddRounded/>} onClick={openCreate} sx={{textTransform:'none',fontWeight:800}}>Add resource</Button>
  </Stack>
  {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
  <Card variant="outlined" sx={{borderRadius:3,mb:2}}><CardContent><Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'1fr 1fr'},gap:2}}>
   <FormControl size="small"><InputLabel>Batch</InputLabel><Select label="Batch" value={batchId} onChange={e=>setBatchId(e.target.value)}><MenuItem value="">All batches</MenuItem>{batches.map(x=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></FormControl>
   <FormControl size="small"><InputLabel>Subject</InputLabel><Select label="Subject" value={subjectId} onChange={e=>setSubjectId(e.target.value)}><MenuItem value="">All subjects</MenuItem>{subjects.map(x=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></FormControl>
  </Box></CardContent></Card>
  {!items.length?<Card variant="outlined" sx={{borderRadius:3}}><CardContent sx={{py:6,textAlign:'center'}}><MenuBookRounded sx={{fontSize:42,color:'#0f766e'}}/><Typography sx={{fontWeight:900,fontSize:18,mt:1}}>No learning resources yet</Typography><Typography sx={{color:'#64748b',mt:.5}}>Add a resource and publish it to a batch.</Typography></CardContent></Card>:
  <Card variant="outlined" sx={{borderRadius:3,overflow:'hidden'}}><Table size="small"><TableHead><TableRow><TableCell>Resource</TableCell><TableCell>Type</TableCell><TableCell>Batch</TableCell><TableCell>Subject</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{items.map(x=><TableRow key={x.id} hover><TableCell><Typography sx={{fontWeight:800}}>{x.title}</Typography><Typography sx={{fontSize:11,color:'#64748b'}}>{x.topic||x.description||x.resource_url}</Typography></TableCell><TableCell><Chip size="small" label={x.resource_type}/></TableCell><TableCell>{x.batch_name||'All batches'}</TableCell><TableCell>{x.subject_name||'General'}</TableCell><TableCell><Chip size="small" label={x.published?'Published':'Draft'} color={x.published?'success':'default'}/></TableCell><TableCell align="right"><Tooltip title="Open"><IconButton size="small" component="a" href={x.resource_url} target="_blank" rel="noreferrer"><OpenInNewRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Edit"><IconButton size="small" onClick={()=>openEdit(x)}><EditRounded fontSize="small"/></IconButton></Tooltip><Tooltip title="Delete"><IconButton size="small" onClick={()=>remove(x.id)}><DeleteOutlineRounded fontSize="small"/></IconButton></Tooltip></TableCell></TableRow>)}</TableBody></Table></Card>}
  <Dialog open={open} onClose={()=>setOpen(false)} fullWidth maxWidth="sm"><DialogTitle sx={{fontWeight:900}}>{editing?'Edit resource':'Add learning resource'}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <TextField size="small" label="Title" value={form.title} onChange={e=>setForm({...form,title:e.target.value})}/>
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'1fr 1fr'},gap:2}}>
    <FormControl size="small"><InputLabel>Type</InputLabel><Select label="Type" value={form.resourceType} onChange={e=>setForm({...form,resourceType:e.target.value})}>{types.map(x=><MenuItem key={x} value={x}>{x}</MenuItem>)}</Select></FormControl>
    <TextField size="small" label="Topic" value={form.topic} onChange={e=>setForm({...form,topic:e.target.value})}/>
   </Box>
   <TextField size="small" label="Resource URL" placeholder="https://..." value={form.resourceUrl} onChange={e=>setForm({...form,resourceUrl:e.target.value})}/>
   <TextField size="small" label="Description" multiline minRows={2} value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/>
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'1fr 1fr'},gap:2}}>
    <FormControl size="small"><InputLabel>Batch</InputLabel><Select label="Batch" value={form.batchId} onChange={e=>setForm({...form,batchId:e.target.value})}><MenuItem value="">All batches</MenuItem>{batches.map(x=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></FormControl>
    <FormControl size="small"><InputLabel>Subject</InputLabel><Select label="Subject" value={form.subjectId} onChange={e=>setForm({...form,subjectId:e.target.value})}><MenuItem value="">General</MenuItem>{subjects.map(x=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></FormControl>
   </Box>
   <FormControl size="small"><InputLabel>Teacher</InputLabel><Select label="Teacher" value={form.teacherId} onChange={e=>setForm({...form,teacherId:e.target.value})}><MenuItem value="">Unassigned</MenuItem>{teachers.map(x=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select></FormControl>
   <FormControl size="small"><InputLabel>Visibility</InputLabel><Select label="Visibility" value={form.published?'PUBLISHED':'DRAFT'} onChange={e=>setForm({...form,published:e.target.value==='PUBLISHED'})}><MenuItem value="DRAFT">Draft</MenuItem><MenuItem value="PUBLISHED">Published</MenuItem></Select></FormControl>
  </Stack></DialogContent><DialogActions><Button onClick={()=>setOpen(false)}>Cancel</Button><Button variant="contained" disabled={!form.title.trim()||!form.resourceUrl.trim()} onClick={save}>Save resource</Button></DialogActions></Dialog>
 </Box>
}
