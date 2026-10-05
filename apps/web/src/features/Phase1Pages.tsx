// @ts-nocheck
import {useEffect,useMemo,useRef,useState} from 'react';
import {api} from '../services';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Divider,IconButton,InputAdornment,MenuItem,Select,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Typography} from '@mui/material';
import {AddRounded,DeleteOutlineRounded,EditRounded,RefreshRounded,SearchRounded,PaymentsRounded,EventAvailableRounded,PeopleRounded,TrendingUpRounded} from '@mui/icons-material';

const unwrap=(r:any)=>r?.data?.data??r?.data;
const err=(e:any)=>{const status=e?.response?.status; const body=e?.response?.data; const message=body?.error?.message??body?.message??body?.error??(typeof body==='string'?body:''); return [(status ? 'HTTP '+status : ''),message].filter(Boolean).join(': ')||e?.message||'Request failed';};

function useList(endpoint:string){
 const [rows,setRows]=useState<any[]>([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
 const load=async()=>{setLoading(true);setError('');try{const r=await api.get(endpoint);const d=unwrap(r);setRows(Array.isArray(d)?d:(d?.data??[]));}catch(e){setError(err(e));}finally{setLoading(false);}};
 useEffect(()=>{load();},[endpoint]); return {rows,loading,error,load,setRows};
}
function Busy({loading,error}:{loading:boolean;error:string}){if(loading)return <Typography sx={{color:'text.secondary',py:4,textAlign:'center'}}>Loading live data…</Typography>;if(error)return <Alert severity="error">{error}</Alert>;return null;}
function Stat({label,value,caption,icon:Icon,trend}:{label:string;value:any;caption?:string;icon:any;trend?:string}){
 return <Card sx={{height:'100%',position:'relative',overflow:'hidden'}}>
  <CardContent sx={{p:{xs:2,md:2.5}}}>
   <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
    <Box sx={{width:38,height:38,borderRadius:2.2,bgcolor:'#e8f8f5',color:'#0f766e',display:'grid',placeItems:'center'}}><Icon sx={{fontSize:19}}/></Box>
    {trend&&<Chip size="small" label={trend} sx={{bgcolor:'#f0fdf4',color:'#15803d',fontSize:10}}/>}
   </Stack>
   <Typography sx={{fontSize:12,color:'#64748b',mt:2}}>{label}</Typography>
   <Typography sx={{fontSize:{xs:24,md:28},fontWeight:950,letterSpacing:-.8,mt:.25}}>{value}</Typography>
   {caption&&<Typography sx={{fontSize:11,color:'#94a3b8',mt:.35}}>{caption}</Typography>}
  </CardContent>
 </Card>
}

export function LiveDashboard(){
 const [data,setData]=useState<any>({}),[loading,setLoading]=useState(true),[error,setError]=useState('');
 const load=async()=>{setLoading(true);try{setData(unwrap(await api.get('/dashboard/summary'))||{});setError('')}catch(e){setError(err(e))}finally{setLoading(false)}};useEffect(()=>{load()},[]);
 if(error)return <Alert severity="error" sx={{borderRadius:3}}>{error}</Alert>;
 return <Stack spacing={2.5}>
  <Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" alignItems={{sm:'center'}} gap={1}>
   <Box><Typography sx={{fontSize:12,color:'#94a3b8',fontWeight:700}}>TODAY AT A GLANCE</Typography><Typography sx={{fontSize:13,color:'#64748b',mt:.25}}>Monitor the institute from one place.</Typography></Box>
   <Button variant="outlined" startIcon={<RefreshRounded/>} onClick={load} sx={{alignSelf:{xs:'stretch',sm:'auto'}}}>Refresh data</Button>
  </Stack>
  {loading?<Card><CardContent><Typography sx={{py:8,textAlign:'center',color:'#64748b'}}>Loading institute command center…</Typography></CardContent></Card>:<>
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',lg:'repeat(4,1fr)'},gap:1.75}}>
    <Stat label="Active Students" value={data.students??0} caption="Current tenant" icon={PeopleRounded} trend="LIVE"/>
    <Stat label="Today's Attendance" value={`${data.attendance??0}%`} caption={`${data.todaySessions??0} sessions today`} icon={EventAvailableRounded}/>
    <Stat label="Collected" value={money(data.collected)} caption="Across recorded invoices" icon={PaymentsRounded}/>
    <Stat label="Outstanding" value={money(data.outstanding)} caption={`${data.overdue??0} overdue invoices`} icon={TrendingUpRounded}/>
   </Box>
   <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'minmax(0,1.45fr) minmax(300px,.75fr)'},gap:1.75}}>
    <Card>
     <CardContent sx={{p:0}}>
      <Box sx={{px:{xs:2,md:2.5},py:2.25,borderBottom:'1px solid #edf1f5'}}>
       <Typography sx={{fontWeight:900,fontSize:15}}>Recent payments</Typography>
       <Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.35}}>Latest activity from your payment ledger</Typography>
      </Box>
      <Box sx={{overflowX:'auto'}}>
       <Table size="small">
        <TableHead><TableRow><TableCell>Student</TableCell><TableCell>Invoice</TableCell><TableCell align="right">Amount</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
        <TableBody>
         {(data.recentPayments||[]).map((p:any)=><TableRow hover key={p.id}>
          <TableCell><Typography sx={{fontWeight:750,fontSize:13}}>{p.first_name} {p.last_name||''}</Typography></TableCell>
          <TableCell sx={{color:'#64748b'}}>{p.invoice_number}</TableCell>
          <TableCell align="right"><Typography sx={{fontWeight:850}}>{money(p.amount)}</Typography></TableCell>
          <TableCell><Chip size="small" label="SUCCESS" color="success" variant="outlined"/></TableCell>
         </TableRow>)}
         {!(data.recentPayments||[]).length&&<TableRow><TableCell colSpan={4}><Typography sx={{py:4,textAlign:'center',color:'#94a3b8'}}>No payments recorded yet.</Typography></TableCell></TableRow>}
        </TableBody>
       </Table>
      </Box>
     </CardContent>
    </Card>
    <Card>
     <CardContent sx={{p:{xs:2,md:2.5}}}>
      <Typography sx={{fontWeight:900,fontSize:15}}>Institute health</Typography>
      <Typography sx={{fontSize:11.5,color:'#94a3b8',mt:.35,mb:2}}>Key operational indicators</Typography>
      <Stack spacing={1}>
       {[['Active batches',data.batches,'Batches currently running'],['Open alerts',data.alerts,'Needs attention'],['Overdue invoices',data.overdue,'Requires follow-up']].map(([a,b,c])=>
        <Box key={a as string} sx={{p:1.4,bgcolor:'#f8fafc',border:'1px solid #eef2f6',borderRadius:2.25}}>
         <Stack direction="row" justifyContent="space-between" alignItems="center"><Typography sx={{fontSize:12.5,fontWeight:700}}>{a as string}</Typography><Typography sx={{fontWeight:950,fontSize:17}}>{b as any}</Typography></Stack>
         <Typography sx={{fontSize:10.5,color:'#94a3b8',mt:.3}}>{c as string}</Typography>
        </Box>
       )}
      </Stack>
     </CardContent>
    </Card>
   </Box>
  </>}
 </Stack>
}

const money=(n:any)=>`₹${Number(n||0).toLocaleString('en-IN',{maximumFractionDigits:2})}`;

export function LiveStudentsPage(){
 const {rows,loading,error,load}=useList('/students/operational'); const [q,setQ]=useState(''); const [open,setOpen]=useState(false);const [edit,setEdit]=useState<any>(null); const [enroll,setEnroll]=useState<any>(null); const [importOpen,setImportOpen]=useState(false);
 const filtered=useMemo(()=>rows.filter(r=>JSON.stringify(r).toLowerCase().includes(q.toLowerCase())),[rows,q]);
 return <Stack spacing={2.5}><Stack direction={{xs:'column',sm:'row'}} gap={1.5}><TextField size="small" fullWidth placeholder="Search name, admission number, phone…" value={q} onChange={e=>setQ(e.target.value)} slotProps={{input:{startAdornment:<InputAdornment position="start"><SearchRounded fontSize="small"/></InputAdornment>}}}/><Button variant="outlined" startIcon={<RefreshRounded/>} onClick={load}>Refresh</Button><Button variant="outlined" onClick={()=>setImportOpen(true)}>Import CSV</Button><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>{setEdit(null);setOpen(true)}}>Add student</Button></Stack><Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent sx={{p:0,overflow:'auto'}}><Busy loading={loading} error={error}/>{!loading&&!error&&<Table><TableHead><TableRow>{['Admission','Student','Class / Batch','Phone','Email','Status',''].map(x=><TableCell key={x} sx={{fontWeight:900,fontSize:11}}>{x}</TableCell>)}</TableRow></TableHead><TableBody>{filtered.map(r=>{
 const admission=r.admissionNumber??r.admission_number??'—';
 const first=r.firstName??r.first_name??'';
 const last=r.lastName??r.last_name??'';
 const className=r.className??r.class_name;
 const batchName=r.batchName??r.batch_name;
 const status=r.status??'ACTIVE';
 return <TableRow hover key={r.id}>
  <TableCell><Typography sx={{fontWeight:750}}>{admission}</Typography></TableCell>
  <TableCell><Typography sx={{fontWeight:800}}>{first} {last}</Typography></TableCell>
  <TableCell>{className?className+' • '+(batchName||'No batch'):'Not enrolled'}</TableCell>
  <TableCell>{r.phone||'—'}</TableCell>
  <TableCell>{r.email||'—'}</TableCell>
  <TableCell><Chip size="small" label={status} color={status==='ACTIVE'?'success':'default'} variant="outlined"/></TableCell>
  <TableCell align="right">
   <Button size="small" onClick={()=>setEnroll(r)}>Enroll</Button>
   <IconButton onClick={()=>{setEdit(r);setOpen(true)}}><EditRounded/></IconButton>
   <IconButton color="error" onClick={async()=>{if(confirm('Archive this student?')){await api.delete(`/students/${r.id}`);load()}}}><DeleteOutlineRounded/></IconButton>
  </TableCell>
 </TableRow>
})}</TableBody></Table>}</CardContent></Card><StudentDialog open={open} initial={edit} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);load()}}/><StudentImportDialog open={importOpen} onClose={()=>setImportOpen(false)} onImported={()=>{setImportOpen(false);load()}}/><EnrollmentDialog open={!!enroll} student={enroll} onClose={()=>setEnroll(null)} onSaved={()=>{setEnroll(null);load()}}/></Stack>
}

function StudentDialog({open,initial,onClose,onSaved}:{open:boolean;initial:any;onClose:()=>void;onSaved:()=>void}){
 const [form,setForm]=useState<any>({admissionNumber:'',firstName:'',lastName:'',phone:'',email:'',dateOfBirth:'',gender:'',address:'',status:'ACTIVE'});const [saving,setSaving]=useState(false);const [error,setError]=useState('');
 useEffect(()=>{
  if(initial){
    setForm({
      admissionNumber: initial.admissionNumber ?? initial.admission_number ?? '',
      firstName: initial.firstName ?? initial.first_name ?? '',
      lastName: initial.lastName ?? initial.last_name ?? '',
      phone: initial.phone ?? '',
      email: initial.email ?? '',
      dateOfBirth: initial.dateOfBirth ?? initial.date_of_birth ?? '',
      gender: initial.gender ?? '',
      address: initial.address ?? '',
      status: initial.status ?? 'ACTIVE'
    });
  }else{
    setForm({admissionNumber:'',firstName:'',lastName:'',phone:'',email:'',dateOfBirth:'',gender:'',address:'',status:'ACTIVE'});
  }
  setError('')
},[initial,open]);
 const save=async()=>{
  setSaving(true);setError('');
  try{
    const payload={
      admissionNumber:String(form.admissionNumber??'').trim(),
      firstName:String(form.firstName??'').trim(),
      lastName:String(form.lastName??'').trim()||null,
      phone:String(form.phone??'').trim()||null,
      email:String(form.email??'').trim()||null,
      dateOfBirth:form.dateOfBirth||null,
      gender:String(form.gender??'').trim()||null,
      address:String(form.address??'').trim()||null,
      status:form.status||'ACTIVE'
    };
    if(!payload.admissionNumber||!payload.firstName){setError('Admission number and first name are required.');return;}
    if(initial) await api.put(`/students/${initial.id}`,payload); else await api.post('/students',payload);
    onSaved();
  }catch(e){setError(err(e))}finally{setSaving(false)}
};const f=(k:string)=>(e:any)=>setForm((x:any)=>({...x,[k]:e.target.value}));
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="md"><DialogTitle>{initial?'Edit student':'Add student'}</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}<Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'1fr 1fr'},gap:2}}>{[['admissionNumber','Admission number'],['firstName','First name'],['lastName','Last name'],['phone','Phone'],['email','Email'],['dateOfBirth','Date of birth'],['gender','Gender'],['address','Address']].map(([k,l])=><TextField key={k} label={l} value={form[k]||''} onChange={f(k)} type={k==='dateOfBirth'?'date':'text'} slotProps={k==='dateOfBirth'?{inputLabel:{shrink:true}}:undefined}/>)}</Box><Select value={form.status} onChange={f('status')}><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem><MenuItem value="WITHDRAWN">Withdrawn</MenuItem></Select></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save} disabled={saving}>{saving?'Saving…':'Save student'}</Button></DialogActions></Dialog>
}

function EnrollmentDialog({open,student,onClose,onSaved}:{open:boolean;student:any;onClose:()=>void;onSaved:()=>void}){
 const {rows:years}=useList('/academic-years');
 const {rows:classes}=useList('/classes');
 const {rows:streams}=useList('/streams');
 const {rows:batches}=useList('/batches');
 const {rows:programs}=useList('/programs');
 const [f,setF]=useState<any>({academicYearId:'',classId:'',streamId:'',batchId:'',programId:''});
 const [error,setError]=useState('');
 useEffect(()=>{
   setF({
     academicYearId:student?.academicYearId||years.find((x:any)=>x.isCurrent)?.id||years[0]?.id||'',
     classId:student?.classId||'',
     streamId:student?.streamId||'',
     batchId:student?.batchId||'',
     programId:student?.programId||''
   });
   setError('');
 },[open,student,years]);
 const save=async()=>{
   setError('');
   if(!f.academicYearId||!f.classId||!f.batchId){setError('Academic year, class and batch are required.');return;}
   try{
     await api.post(`/students/${student.id}/enrollment`,{...f,streamId:f.streamId||null,programId:f.programId||null});
     onSaved();
   }catch(e){setError(err(e))}
 };
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>Assign academic enrollment</DialogTitle><DialogContent><Stack spacing={2} mt={1}>
   {error&&<Alert severity="error">{error}</Alert>}
   <Select value={f.academicYearId} displayEmpty onChange={e=>setF((x:any)=>({...x,academicYearId:e.target.value}))}>{years.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
   <Select value={f.classId} displayEmpty onChange={e=>setF((x:any)=>({...x,classId:e.target.value}))}><MenuItem value="">Select class</MenuItem>{classes.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
   <Select value={f.streamId} displayEmpty onChange={e=>setF((x:any)=>({...x,streamId:e.target.value}))}><MenuItem value="">No stream</MenuItem>{streams.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
   <Select value={f.programId} displayEmpty onChange={e=>setF((x:any)=>({...x,programId:e.target.value}))}><MenuItem value="">Use batch program</MenuItem>{programs.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
   <Select value={f.batchId} displayEmpty onChange={e=>setF((x:any)=>({...x,batchId:e.target.value}))}><MenuItem value="">Select batch</MenuItem>{batches.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
 </Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save}>Save enrollment</Button></DialogActions></Dialog>}

function StudentImportDialog({open,onClose,onImported}:{open:boolean;onClose:()=>void;onImported:()=>void}){
 const input=useRef<HTMLInputElement|null>(null); const [file,setFile]=useState<File|null>(null); const [result,setResult]=useState<any>(null); const [busy,setBusy]=useState(false); const [error,setError]=useState('');
 const send=async(commit:boolean)=>{
  if(!file){setError('Choose a CSV file first.');return}
  setBusy(true);setError('');
  try{const body=new FormData();body.append('file',file);const r=await api.post(commit?'/students/import':'/students/import/preview',body,{headers:{'Content-Type':'multipart/form-data'}});setResult(r.data?.data??r.data);if(commit)onImported();}
  catch(e){setError(err(e))}finally{setBusy(false)}
 };
 useEffect(()=>{if(!open){setFile(null);setResult(null);setError('')}},[open]);
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
  <DialogTitle>Import students from CSV</DialogTitle>
  <DialogContent><Stack spacing={2} mt={1}>
   {error&&<Alert severity="error">{error}</Alert>}
   <Card variant="outlined" sx={{p:2}}><Stack direction={{xs:'column',sm:'row'}} spacing={1.5} alignItems={{sm:'center'}}>
    <input ref={input} type="file" accept=".csv,text/csv" hidden onChange={e=>{setFile(e.target.files?.[0]||null);setResult(null)}}/>
    <Button variant="outlined" onClick={()=>input.current?.click()}>Choose CSV</Button>
    <Typography sx={{fontSize:12,color:'#64748b'}}>{file?.name||'Required columns: admissionNumber, firstName. Optional: lastName, email, phone, dateOfBirth, gender, status, address.'}</Typography>
   </Stack></Card>
   {result&&<><Box sx={{display:'grid',gridTemplateColumns:{xs:'repeat(2,1fr)',sm:'repeat(4,1fr)'},gap:1}}>{[['Rows',result.totalRows],['Valid',result.validRows],['Errors',result.failedRows],['Imported',result.importedRows]].map(([k,v])=><Card key={k as string} variant="outlined"><CardContent sx={{p:1.5}}><Typography sx={{fontSize:10,color:'#64748b'}}>{k as string}</Typography><Typography sx={{fontSize:22,fontWeight:900}}>{v as any}</Typography></CardContent></Card>)}</Box>
    {(result.errors||[]).length>0&&<Alert severity="warning">Fix the listed rows and preview again before importing.</Alert>}
    {(result.preview||[]).length>0&&<Box sx={{overflowX:'auto'}}><Table size="small"><TableHead><TableRow>{['Admission','First name','Last name','Phone','Email'].map(x=><TableCell key={x} sx={{fontWeight:900,fontSize:11}}>{x}</TableCell>)}</TableRow></TableHead><TableBody>{result.preview.slice(0,8).map((r:any,i:number)=><TableRow key={i}><TableCell>{r.admissionNumber}</TableCell><TableCell>{r.firstName}</TableCell><TableCell>{r.lastName||'—'}</TableCell><TableCell>{r.phone||'—'}</TableCell><TableCell>{r.email||'—'}</TableCell></TableRow>)}</TableBody></Table></Box>}
   </>}
  </Stack></DialogContent>
  <DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="outlined" disabled={!file||busy} onClick={()=>send(false)}>Preview & Validate</Button><Button variant="contained" disabled={!file||busy||!result||result.failedRows>0} onClick={()=>send(true)}>Import valid students</Button></DialogActions>
 </Dialog>
}

export function LiveStudentProfile({id}:{id:string}){
 const [student,setStudent]=useState<any>(null),[invoices,setInvoices]=useState<any[]>([]),[loading,setLoading]=useState(true);
 useEffect(()=>{(async()=>{try{const [s,f]=await Promise.all([api.get(`/students/${id}`),api.get('/finance/invoices',{params:{studentId:id}})]);setStudent(unwrap(s));setInvoices(unwrap(f)||[])}finally{setLoading(false)}})()},[id]);
 if(loading)return <Typography>Loading student…</Typography>; if(!student)return <Alert severity="error">Student not found.</Alert>;
 return <Stack spacing={2.5}><Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent><Typography sx={{fontSize:12,color:'text.secondary'}}>Student record</Typography><Typography variant="h5" sx={{fontWeight:900}}>{student.firstName} {student.lastName||''}</Typography><Typography color="text.secondary">{student.admissionNumber} • {student.phone||'No phone'} • {student.email||'No email'}</Typography></CardContent></Card><Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',md:'repeat(3,1fr)'},gap:2}}><Stat label="Status" value={student.status} icon={PeopleRounded}/><Stat label="Phone" value={student.phone||'—'} icon={PeopleRounded}/><Stat label="Invoices" value={invoices.length} icon={PaymentsRounded}/></Box><Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent><Typography sx={{fontWeight:900,mb:1.5}}>Fee ledger</Typography><Table size="small"><TableHead><TableRow><TableCell>Invoice</TableCell><TableCell>Due</TableCell><TableCell>Amount</TableCell><TableCell>Paid</TableCell><TableCell>Status</TableCell></TableRow></TableHead><TableBody>{invoices.map(i=><TableRow key={i.id}><TableCell>{i.invoice_number}</TableCell><TableCell>{i.due_date}</TableCell><TableCell>{money(i.amount)}</TableCell><TableCell>{money(i.paid_amount)}</TableCell><TableCell><Chip size="small" label={i.status} variant="outlined"/></TableCell></TableRow>)}</TableBody></Table></CardContent></Card></Stack>
}

export function LiveAcademicsPage(){
 const resources=[['Academic years','/academic-years',['name','startDate','endDate','current','status']],['Classes','/classes',['name','displayOrder','active']],['Streams','/streams',['name','code','active']],['Subjects','/subjects',['name','code','active']],['Batches','/batches',['name','code','capacity','status']]] as const;
 const [tab,setTab]=useState(0); const [refresh,setRefresh]=useState(0);
 return <Stack spacing={2.5}><Box sx={{display:'flex',gap:1,overflow:'auto'}}>{resources.map((r,i)=><Button key={r[0]} variant={tab===i?'contained':'outlined'} onClick={()=>setTab(i)}>{r[0]}</Button>)}</Box><AcademicResource key={refresh} title={resources[tab][0]} endpoint={resources[tab][1]} fields={[...resources[tab][2]]} onChanged={()=>setRefresh(x=>x+1)}/></Stack>
}
function AcademicResource({title,endpoint,fields,onChanged}:{title:string;endpoint:string;fields:string[];onChanged:()=>void}){
 const {rows,loading,error,load}=useList(endpoint);const [open,setOpen]=useState(false);const [edit,setEdit]=useState<any>(null);
 const labels:any={name:'Name',startDate:'Start date',endDate:'End date',current:'Current',status:'Status',displayOrder:'Display order',active:'Active',code:'Code',capacity:'Capacity'};
 const value=(r:any,f:string)=>{const v=r[f]??(f==='current'?r.isCurrent:undefined);if(v===null||v===undefined||v==='')return '—';if(typeof v==='boolean')return v?'Yes':'No';return String(v)};
 return <Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent sx={{p:0}}>
  <Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" alignItems={{sm:'center'}} gap={1} sx={{px:{xs:2,md:2.5},py:2}}><Box><Typography fontWeight={900}>{title}</Typography><Typography fontSize={12} color="text.secondary">{rows.length} records in this tenant</Typography></Box><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>{setEdit(null);setOpen(true)}}>Add</Button></Stack>
  <Busy loading={loading} error={error}/>{!loading&&!error&&<Box sx={{overflowX:'auto'}}><Table size="small"><TableHead><TableRow>{fields.map(f=><TableCell key={f} sx={{fontWeight:900,fontSize:11}}>{labels[f]||f}</TableCell>)}<TableCell align="right">Actions</TableCell></TableRow></TableHead>
  <TableBody>{rows.map(r=><TableRow hover key={r.id}>{fields.map(f=><TableCell key={f}>{value(r,f)}</TableCell>)}<TableCell align="right"><IconButton aria-label="Edit" onClick={()=>{setEdit(r);setOpen(true)}}><EditRounded fontSize="small"/></IconButton><IconButton aria-label="Delete" color="error" onClick={async()=>{if(confirm('Delete this record?')){try{await api.delete(endpoint+'/'+r.id);load();onChanged()}catch(e){alert(err(e))}}}}><DeleteOutlineRounded fontSize="small"/></IconButton></TableCell></TableRow>)}{!rows.length&&<TableRow><TableCell colSpan={fields.length+1}><Typography sx={{py:5,textAlign:'center',color:'text.secondary'}}>No records found. Click Add to create one.</Typography></TableCell></TableRow>}</TableBody></Table></Box>}
  {endpoint==='/batches'?<BatchDialog open={open} initial={edit} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);load();onChanged()}}/>:<AcademicDialog open={open} initial={edit} endpoint={endpoint} fields={fields} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);load();onChanged()}}/>}
 </CardContent></Card>
}
function AcademicDialog({open,initial,endpoint,fields,onClose,onSaved}:{open:boolean;initial:any;endpoint:string;fields:string[];onClose:()=>void;onSaved:()=>void}){
 const [form,setForm]=useState<any>({});const [saving,setSaving]=useState(false),[error,setError]=useState('');const labels:any={name:'Name',startDate:'Start date',endDate:'End date',current:'Current',status:'Status',displayOrder:'Display order',active:'Active',code:'Code'};
 useEffect(()=>{const x:any={};fields.forEach(f=>x[f]=initial?.[f]??(f==='current'?(initial?.current??initial?.isCurrent??false):f==='active'?true:f==='status'?'ACTIVE':''));setForm(x);setError('')},[initial,open,fields.join(',')]);
 const set=(k:string)=>(e:any)=>setForm((x:any)=>({...x,[k]:e.target.value}));
 const save=async()=>{setSaving(true);setError('');try{const payload={...form};if('displayOrder' in payload)payload.displayOrder=Number(payload.displayOrder||0);if(initial)await api.put(endpoint+'/'+initial.id,payload);else await api.post(endpoint,payload);onSaved()}catch(e){setError(err(e))}finally{setSaving(false)}};
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>{initial?'Edit':'Add'} {endpoint==='/academic-years'?'academic year':endpoint==='/classes'?'class':endpoint==='/streams'?'stream':'subject'}</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}{fields.map(f=>{if(f==='active'||f==='current')return <FormControl key={f} fullWidth><InputLabel>{labels[f]}</InputLabel><Select label={labels[f]} value={String(form[f])} onChange={e=>setForm((x:any)=>({...x,[f]:e.target.value==='true'}))}><MenuItem value="true">{f==='current'?'Current':'Active'}</MenuItem><MenuItem value="false">{f==='current'?'Not current':'Inactive'}</MenuItem></Select></FormControl>;if(f==='status')return <FormControl key={f} fullWidth><InputLabel>Status</InputLabel><Select label="Status" value={form[f]||'ACTIVE'} onChange={set(f)}><MenuItem value="ACTIVE">ACTIVE</MenuItem><MenuItem value="ARCHIVED">ARCHIVED</MenuItem></Select></FormControl>;const type=f==='startDate'||f==='endDate'?'date':f==='displayOrder'?'number':'text';return <TextField key={f} label={labels[f]||f} value={form[f]??''} onChange={set(f)} type={type} slotProps={type==='date'?{inputLabel:{shrink:true}}:undefined}/>})}</Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" disabled={saving} onClick={save}>{saving?'Saving…':'Save'}</Button></DialogActions></Dialog>
}
function BatchDialog({open,initial,onClose,onSaved}:{open:boolean;initial:any;onClose:()=>void;onSaved:()=>void}){
 const {rows:years}=useList('/academic-years');
 const {rows:classes}=useList('/classes');
 const {rows:streams}=useList('/streams');
 const [form,setForm]=useState<any>({academicYearId:'',classId:'',streamId:'',name:'',code:'',capacity:'',status:'ACTIVE'});
 const [saving,setSaving]=useState(false),[error,setError]=useState('');
 useEffect(()=>{
   const currentYear=years.find((x:any)=>x.isCurrent)?.id||years[0]?.id||'';
   setForm({
     academicYearId:initial?.academicYearId||initial?.academic_year_id||currentYear,
     classId:initial?.classId||initial?.class_id||'',
     streamId:initial?.streamId||initial?.stream_id||'',
     name:initial?.name||'', code:initial?.code||'', capacity:initial?.capacity??'', status:initial?.status||'ACTIVE'
   });
   setError('');
 },[open,initial,years]);
 const set=(key:string)=>(e:any)=>setForm((x:any)=>({...x,[key]:e.target.value}));
 const save=async()=>{
   if(!form.academicYearId||!form.classId||!form.name.trim()){setError('Academic year, class and batch name are required.');return;}
   setSaving(true);setError('');
   try{
     const payload={...form,streamId:form.streamId||null,capacity:form.capacity===''?null:Number(form.capacity)};
     if(initial) await api.put(`/batches/${initial.id}`,payload); else await api.post('/batches',payload);
     onSaved();
   }catch(e){setError(err(e))}finally{setSaving(false)}
 };
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
   <DialogTitle>{initial?'Edit':'Add'} batch</DialogTitle>
   <DialogContent><Stack spacing={2} mt={1}>
     {error&&<Alert severity="error">{error}</Alert>}
     <Select value={form.academicYearId} displayEmpty onChange={set('academicYearId')}><MenuItem value="" disabled>Select academic year</MenuItem>{years.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}{x.isCurrent?' • Current':''}</MenuItem>)}</Select>
     <Select value={form.classId} displayEmpty onChange={set('classId')}><MenuItem value="" disabled>Select class</MenuItem>{classes.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
     <Select value={form.streamId} displayEmpty onChange={set('streamId')}><MenuItem value="">No stream</MenuItem>{streams.map((x:any)=><MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}</Select>
     <TextField label="Batch name" value={form.name} onChange={set('name')} required/>
     <TextField label="Code" value={form.code} onChange={set('code')}/>
     <TextField label="Capacity" type="number" value={form.capacity} onChange={set('capacity')}/>
     <Select value={form.status} onChange={set('status')}><MenuItem value="ACTIVE">ACTIVE</MenuItem><MenuItem value="INACTIVE">INACTIVE</MenuItem></Select>
   </Stack></DialogContent>
   <DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" disabled={saving} onClick={save}>{saving?'Saving…':'Save batch'}</Button></DialogActions>
 </Dialog>;
}

export function LiveAttendancePage(){
 const {rows:batches,loading:batchLoading}=useList('/batches');
 const {rows:subjects}=useList('/subjects');
 const [batchId,setBatchId]=useState(''); const [date,setDate]=useState(new Date().toISOString().slice(0,10));
 const [sessions,setSessions]=useState<any[]>([]); const [session,setSession]=useState<any>(null); const [records,setRecords]=useState<any[]>([]); const [summary,setSummary]=useState<any>({});
 const [creating,setCreating]=useState(false),[saving,setSaving]=useState(false),[error,setError]=useState('');
 const [creatingSubject,setCreatingSubject]=useState(''),[creatingStart,setCreatingStart]=useState(''),[creatingEnd,setCreatingEnd]=useState('');
 useEffect(()=>{if(!batchId&&batches[0]?.id)setBatchId(batches[0].id)},[batches,batchId]);
 const load=async()=>{if(!batchId)return;setError('');try{const list=unwrap(await api.get('/attendance/sessions',{params:{batchId,date}}))||[];setSessions(list);const current=session?.id?list.find((x:any)=>x.id===session.id):list[0];setSession(current||null);if(current){const [r,s]=await Promise.all([api.get('/attendance/sessions/'+current.id+'/records'),api.get('/attendance/sessions/'+current.id+'/summary')]);setRecords(unwrap(r)||[]);setSummary(unwrap(s)||{})}else{setRecords([]);setSummary({})}}catch(e){setError(err(e))}};
 useEffect(()=>{load()},[batchId,date]);
 const createSession=async()=>{setSaving(true);setError('');try{await api.post('/attendance/sessions',{batchId,subjectId:creatingSubject||null,sessionDate:date,startTime:creatingStart||null,endTime:creatingEnd||null});setCreating(false);setCreatingSubject('');setCreatingStart('');setCreatingEnd('');await load()}catch(e){setError(err(e))}finally{setSaving(false)}};
 const save=async()=>{if(!session||session.status!=='OPEN')return;setSaving(true);setError('');try{await api.post('/attendance/sessions/'+session.id+'/records/bulk',{records:records.map(r=>({studentId:r.student_id,status:r.status}))});await load()}catch(e){setError(err(e))}finally{setSaving(false)}};
 const closeSession=async()=>{if(!session||session.status!=='OPEN')return;setSaving(true);setError('');try{await api.post('/attendance/sessions/'+session.id+'/close');await load()}catch(e){setError(err(e))}finally{setSaving(false)}};
 const setAll=(status:string)=>setRecords(x=>x.map(r=>({...r,status})));
 const present=Number(summary.present||0),absent=Number(summary.absent||0),late=Number(summary.late||0),leave=Number(summary.leave||0),unmarked=Number(summary.unmarked||0),total=Number(summary.total||records.length);
 const percentage=total?Math.round(((present+late)/total)*100):0;
 return <Stack spacing={2.5}>
  <Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent><Stack direction={{xs:'column',lg:'row'}} gap={1.5} alignItems={{lg:'center'}}>
   <Select size="small" value={batchId} onChange={e=>{setBatchId(e.target.value as string);setSession(null)}} displayEmpty sx={{minWidth:240}}><MenuItem value="" disabled>{batchLoading?'Loading batches…':'Select batch'}</MenuItem>{batches.map(b=><MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}</Select>
   <TextField size="small" type="date" value={date} onChange={e=>{setDate(e.target.value);setSession(null)}} slotProps={{inputLabel:{shrink:true}}}/>
   <Select size="small" value={session?.id||''} onChange={e=>setSession(sessions.find(x=>x.id===e.target.value)||null)} displayEmpty sx={{minWidth:260}}><MenuItem value="" disabled>{sessions.length?'Select session':'No sessions for this date'}</MenuItem>{sessions.map(s=><MenuItem key={s.id} value={s.id}>{s.subject_name}{s.start_time?' • '+s.start_time:''}</MenuItem>)}</Select>
   <Box flex={1}/><Button variant="outlined" onClick={()=>setCreating(true)} disabled={!batchId}>Create session</Button><Button variant="contained" onClick={save} disabled={!session||session.status!=='OPEN'||saving}>{saving?'Saving…':'Save attendance'}</Button>
  </Stack></CardContent></Card>
  {error&&<Alert severity="error">{error}</Alert>}
  {session&&<Box sx={{display:'grid',gridTemplateColumns:{xs:'repeat(2,1fr)',md:'repeat(6,1fr)'},gap:1.5}}>{[['Total',total],['Present',present],['Absent',absent],['Late',late],['Leave',leave],['Attendance',percentage+'%']].map(([a,b])=><Stat key={String(a)} label={String(a)} value={b} icon={EventAvailableRounded}/>)}</Box>}
  {session&&<Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent>
   <Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" gap={1} mb={1.5}><Box><Typography sx={{fontWeight:900}}>{session.batch_name} • {session.subject_name}</Typography><Typography sx={{fontSize:12,color:'text.secondary'}}>{date}{session.start_time?' • '+session.start_time:''} • {session.status||'OPEN'}</Typography></Box><Stack direction="row" gap={1} flexWrap="wrap"><Button size="small" variant="outlined" onClick={()=>setAll('PRESENT')} disabled={session.status!=='OPEN'}>Mark all present</Button><Button size="small" variant="outlined" onClick={()=>setAll('ABSENT')} disabled={session.status!=='OPEN'}>Mark all absent</Button><Button size="small" variant="contained" onClick={closeSession} disabled={session.status!=='OPEN'||saving}>Close session</Button></Stack></Stack>
   {unmarked>0&&<Alert severity="warning" sx={{mb:1.5}}>{unmarked} student(s) are still unmarked.</Alert>}
   {records.map((r,i)=><Stack key={r.student_id} direction="row" alignItems="center" gap={2} py={1.2} sx={{borderTop:'1px solid #f1f5f9'}}><Box flex={1}><Typography sx={{fontWeight:800}}>{r.first_name} {r.last_name||''}</Typography><Typography sx={{fontSize:11,color:'text.secondary'}}>{r.admission_number}</Typography></Box><Select size="small" value={r.status||'UNMARKED'} disabled={session.status!=='OPEN'} onChange={e=>setRecords(x=>x.map((z,j)=>j===i?{...z,status:e.target.value}:z))} sx={{minWidth:145}}>{['UNMARKED','PRESENT','ABSENT','LATE','LEAVE'].map(v=><MenuItem key={v} value={v}>{v==='UNMARKED'?'Not marked':v}</MenuItem>)}</Select></Stack>)}
   {!records.length&&<Typography color="text.secondary">No enrolled students in this batch.</Typography>}
  </CardContent></Card>}
  <Dialog open={creating} onClose={()=>setCreating(false)} fullWidth maxWidth="sm"><DialogTitle>Create attendance session</DialogTitle><DialogContent><Stack spacing={2} mt={1}><Typography sx={{fontSize:12,color:'text.secondary'}}>Batch: {batches.find((b:any)=>b.id===batchId)?.name||'—'} • {date}</Typography><Select value={creatingSubject} displayEmpty onChange={e=>setCreatingSubject(e.target.value as string)}><MenuItem value="">General session</MenuItem>{subjects.map(s=><MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}</Select><Box sx={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:2}}><TextField label="Start time" type="time" value={creatingStart} onChange={e=>setCreatingStart(e.target.value)} slotProps={{inputLabel:{shrink:true}}}/><TextField label="End time" type="time" value={creatingEnd} onChange={e=>setCreatingEnd(e.target.value)} slotProps={{inputLabel:{shrink:true}}}/></Box></Stack></DialogContent><DialogActions><Button onClick={()=>setCreating(false)}>Cancel</Button><Button variant="contained" onClick={createSession} disabled={saving}>Create session</Button></DialogActions></Dialog>
 </Stack>;
}
export function LiveFinancePage(){
 const {rows:invoices,loading,error,load}=useList('/finance/invoices');
 const {rows:students}=useList('/students');
 const [summary,setSummary]=useState<any>({});
 const [pendingPayments,setPendingPayments]=useState<any[]>([]);
 const [open,setOpen]=useState(false),[pay,setPay]=useState<any>(null);
 const refresh=async()=>{load();try{const [s,p]=await Promise.all([api.get('/finance/summary'),api.get('/finance/payments/pending')]);setSummary(unwrap(s)||{});setPendingPayments(unwrap(p)||[])}catch{}};
 useEffect(()=>{refresh()},[]);
 const verify=async(id:string)=>{try{await api.post('/finance/payments/'+id+'/verify');await refresh()}catch(e){alert(err(e))}};
 return <Stack spacing={2.5}>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',md:'repeat(4,1fr)'},gap:2}}>
   <Stat label="Invoices" value={summary.invoices??0} icon={PaymentsRounded}/><Stat label="Billed" value={money(summary.billed)} icon={PaymentsRounded}/><Stat label="Collected" value={money(summary.collected)} icon={PaymentsRounded}/><Stat label="Outstanding" value={money(summary.outstanding)} icon={PaymentsRounded}/>
  </Box>
  <Stack direction="row" justifyContent="flex-end"><Button variant="contained" startIcon={<AddRounded/>} onClick={()=>setOpen(true)}>Create invoice</Button></Stack>
  <Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent sx={{p:0,overflow:'auto'}}><Busy loading={loading} error={error}/>{!loading&&!error&&<Table><TableHead><TableRow>{['Invoice','Student','Amount','Paid','Due','Status',''].map(x=><TableCell key={x} sx={{fontWeight:900,fontSize:11}}>{x}</TableCell>)}</TableRow></TableHead><TableBody>{invoices.map(i=><TableRow key={i.id}><TableCell>{i.invoice_number}</TableCell><TableCell>{i.first_name?i.first_name+' '+(i.last_name||''):i.student_id}</TableCell><TableCell>{money(i.amount)}</TableCell><TableCell>{money(i.paid_amount)}</TableCell><TableCell>{i.due_date}</TableCell><TableCell><Chip size="small" label={i.status} color={i.status==='PAID'?'success':i.status==='PARTIALLY_PAID'?'warning':'default'} variant="outlined"/></TableCell><TableCell align="right">{i.status!=='PAID'&&<Button size="small" onClick={()=>setPay(i)}>Record payment</Button>}</TableCell></TableRow>)}</TableBody></Table>}</CardContent></Card>
  {pendingPayments.length>0&&<Card elevation={0} sx={{border:'1px solid #e5e7eb'}}><CardContent><Typography sx={{fontWeight:900,mb:1.5}}>Pending payment verification</Typography><Table size="small"><TableHead><TableRow><TableCell>Student</TableCell><TableCell>Invoice</TableCell><TableCell>Amount</TableCell><TableCell>Gateway</TableCell><TableCell align="right">Action</TableCell></TableRow></TableHead><TableBody>{pendingPayments.map(p=><TableRow key={p.id}><TableCell>{p.first_name} {p.last_name||''}<Typography sx={{fontSize:10,color:'text.secondary'}}>{p.admission_number}</Typography></TableCell><TableCell>{p.invoice_number}</TableCell><TableCell>{money(p.amount)}</TableCell><TableCell>{p.gateway}</TableCell><TableCell align="right"><Button size="small" variant="contained" onClick={()=>verify(p.id)}>Verify</Button></TableCell></TableRow>)}</TableBody></Table></CardContent></Card>}
  <InvoiceDialog open={open} students={students} onClose={()=>setOpen(false)} onSaved={()=>{setOpen(false);refresh()}}/>
  <PaymentDialog invoice={pay} onClose={()=>setPay(null)} onSaved={()=>{setPay(null);refresh()}}/>
 </Stack>
}
function InvoiceDialog({open,students,onClose,onSaved}:{open:boolean;students:any[];onClose:()=>void;onSaved:()=>void}){const [f,setF]=useState<any>({studentId:'',amount:'',dueDate:new Date().toISOString().slice(0,10),invoiceNumber:''});const [error,setError]=useState('');const save=async()=>{try{await api.post('/finance/invoices',{...f,amount:Number(f.amount),invoiceNumber:f.invoiceNumber||null,installmentId:null});onSaved()}catch(e){setError(err(e))}};return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>Create invoice</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}<Select value={f.studentId} displayEmpty onChange={e=>setF((x:any)=>({...x,studentId:e.target.value}))}><MenuItem value="" disabled>Select student</MenuItem>{students.map(s=><MenuItem key={s.id} value={s.id}>{s.admissionNumber} • {s.firstName} {s.lastName||''}</MenuItem>)}</Select><TextField label="Amount" type="number" value={f.amount} onChange={e=>setF((x:any)=>({...x,amount:e.target.value}))}/><TextField label="Due date" type="date" value={f.dueDate} onChange={e=>setF((x:any)=>({...x,dueDate:e.target.value}))} slotProps={{inputLabel:{shrink:true}}}/><TextField label="Invoice number (optional)" value={f.invoiceNumber} onChange={e=>setF((x:any)=>({...x,invoiceNumber:e.target.value}))}/></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancel</Button><Button variant="contained" onClick={save}>Create</Button></DialogActions></Dialog>}
function PaymentDialog({invoice,onClose,onSaved}:{invoice:any;onClose:()=>void;onSaved:()=>void}){const [amount,setAmount]=useState('');const [error,setError]=useState('');const [pending,setPending]=useState(false);useEffect(()=>{if(invoice){setAmount(String(Number(invoice.amount)-Number(invoice.paid_amount||0)));setPending(false);setError('')}},[invoice]);const save=async()=>{try{setError('');setPending(false);await api.post('/finance/payments',{invoiceId:invoice.id,amount:Number(amount),gateway:'MANUAL',idempotencyKey:crypto.randomUUID()});setPending(true)}catch(e){setError(err(e))}};return <Dialog open={!!invoice} onClose={onClose}><DialogTitle>Record payment</DialogTitle><DialogContent><Stack spacing={2} mt={1}>{error&&<Alert severity="error">{error}</Alert>}{pending&&<Alert severity="info">Payment recorded as pending. An authorized institute user must verify it before the invoice balance is updated.</Alert>}<Typography>Balance: <b>{money(Number(invoice?.amount||0)-Number(invoice?.paid_amount||0))}</b></Typography><TextField label="Payment amount" type="number" value={amount} onChange={e=>setAmount(e.target.value)} disabled={pending}/></Stack></DialogContent><DialogActions><Button onClick={onClose}>{pending?'Close':'Cancel'}</Button>{!pending&&<Button variant="contained" onClick={save}>Submit payment</Button>}</DialogActions></Dialog>}
