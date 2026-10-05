import {useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Grid,Stack,TextField,Typography} from '@mui/material';
import {AddRounded,SettingsRounded} from '@mui/icons-material';
import {api} from '../services';

export default function PlatformInstitutesPage(){
 const[rows,setRows]=useState<any[]>([]),[open,setOpen]=useState(false),[form,setForm]=useState<any>({instituteType:'OTHER',timezone:'Asia/Kolkata'}),[error,setError]=useState(''),[loading,setLoading]=useState(true);
 async function load(){try{setLoading(true);const r=await api.get('/platform/institutes');setRows(r?.data?.data??r?.data??[])}catch(e:any){setError(e?.response?.data?.message||'Unable to load platform institutes')}finally{setLoading(false)}}
 useEffect(()=>{load()},[]);
 async function create(){try{await api.post('/platform/institutes',form);setOpen(false);setForm({instituteType:'OTHER',timezone:'Asia/Kolkata'});await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to create institute')}}
 async function setStatus(id:string,status:string){try{await api.put('/platform/institutes/'+id+'/status',{status});await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to update institute')}}
 return <Stack spacing={2.5}>
  {error&&<Alert severity="error">{error}</Alert>}
  <Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" alignItems={{sm:'center'}}>
   <Box/>
   <Button variant="contained" startIcon={<AddRounded/>} onClick={()=>setOpen(true)}>Add Institute</Button>
  </Stack>
  <Grid container spacing={2}>
   {rows.map(r=><Grid item xs={12} md={6} lg={4} key={r.id}>
    <Card variant="outlined" sx={{height:'100%',borderRadius:3}}>
     <CardContent>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
       <div><Typography sx={{fontWeight:900,fontSize:18}}>{r.name}</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>{r.slug}</Typography></div>
       <Chip size="small" label={(r.setup_status||'DRAFT').replaceAll('_',' ')} />
      </Stack>
      <Stack direction="row" spacing={2} sx={{mt:2}}>
       <Typography sx={{fontSize:12}}><b>{r.programs}</b> programs</Typography><Typography sx={{fontSize:12}}><b>{r.batches}</b> batches</Typography><Typography sx={{fontSize:12}}><b>{r.students}</b> students</Typography>
      </Stack>
      <Stack direction="row" spacing={1} sx={{mt:2}}>
       <Button size="small" variant="outlined" startIcon={<SettingsRounded/>} href={'/admin/institutes/'+r.id}>Open setup</Button>
       <Button size="small" onClick={()=>setStatus(r.id,r.status==='ACTIVE'?'SUSPENDED':'ACTIVE')}>{r.status==='ACTIVE'?'Suspend':'Activate'}</Button>
      </Stack>
    </CardContent>
   </Card></Grid>)}
  </Grid>
  <Dialog open={open} onClose={()=>setOpen(false)} fullWidth maxWidth="sm">
   <DialogTitle sx={{fontWeight:900}}>Create institute</DialogTitle>
   <DialogContent><Stack spacing={2} sx={{pt:1}}>
    <TextField label="Institute name" fullWidth value={form.name||''} onChange={e=>setForm({...form,name:e.target.value})}/>
    <TextField label="Slug" fullWidth value={form.slug||''} onChange={e=>setForm({...form,slug:e.target.value})} helperText="Lowercase letters, numbers and hyphens"/>
    <TextField select fullWidth label="Institute type" value={form.instituteType||'OTHER'} onChange={e=>setForm({...form,instituteType:e.target.value})} SelectProps={{native:true}}><option value="SCHOOL_COACHING">School Coaching</option><option value="COMPETITIVE_COACHING">Competitive Coaching</option><option value="MULTI_EXAM">Multi Exam</option><option value="HYBRID">Hybrid</option><option value="OTHER">Other</option></TextField>
    <TextField label="Contact email" fullWidth value={form.contactEmail||''} onChange={e=>setForm({...form,contactEmail:e.target.value})}/>
    <TextField label="Contact phone" fullWidth value={form.contactPhone||''} onChange={e=>setForm({...form,contactPhone:e.target.value})}/>
   </Stack></DialogContent>
   <DialogActions><Button onClick={()=>setOpen(false)}>Cancel</Button><Button variant="contained" onClick={create}>Create Institute</Button></DialogActions>
  </Dialog>
 </Stack>
}