import {useCallback,useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Checkbox,Chip,Dialog,DialogActions,DialogContent,DialogTitle,FormControlLabel,FormGroup,Stack,TextField,Typography} from '@mui/material';
import {api} from '../services';

type Role={id:string;name:string;code:string;description?:string;system_role:boolean;permission_count:number};
type Permission={code:string;description:string};

export default function RolesPage(){
 const [roles,setRoles]=useState<Role[]>([]),[permissions,setPermissions]=useState<Permission[]>([]);
 const [open,setOpen]=useState(false),[editing,setEditing]=useState<Role|null>(null);
 const [name,setName]=useState(''),[description,setDescription]=useState(''),[selected,setSelected]=useState<string[]>([]);
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('');
 const load=useCallback(async()=>{try{const [r,p]=await Promise.all([api.get('/roles'),api.get('/roles/permissions')]);setRoles(r.data?.data??r.data??[]);setPermissions(p.data?.data??p.data??[]);}catch(e:any){setError(e?.response?.data?.message??'Unable to load roles and permissions.');}},[]);
 useEffect(()=>{void load();},[load]);
 const start=async(role?:Role)=>{setEditing(role??null);setName(role?.name??'');setDescription(role?.description??'');setSelected([]);setError('');setOpen(true);if(role){try{const r=await api.get('/roles/'+role.id);setSelected(r.data?.permissions??[]);}catch(e:any){setError(e?.response?.data?.message??'Unable to load role permissions.');}}};
 const save=async()=>{setBusy(true);setError('');setSuccess('');try{const payload={name,description,permissions:selected};if(editing)await api.put('/roles/'+editing.id,payload);else await api.post('/roles',payload);setOpen(false);setSuccess(editing?'Role updated.':'Custom role created.');await load();}catch(e:any){setError(e?.response?.data?.message??'Unable to save role.');}finally{setBusy(false);}};
 const remove=async(role:Role)=>{if(!window.confirm('Delete this custom role? Roles assigned to users cannot be deleted.'))return;setError('');try{await api.delete('/roles/'+role.id);setSuccess('Role deleted.');await load();}catch(e:any){setError(e?.response?.data?.message??'Unable to delete role.');}};
 const toggle=(code:string)=>setSelected(v=>v.includes(code)?v.filter(x=>x!==code):[...v,code]);
 return <Stack spacing={2}>
  {error&&<Alert severity="error">{error}</Alert>}{success&&<Alert severity="success">{success}</Alert>}
  <Box sx={{display:'flex',justifyContent:'space-between',alignItems:'center',gap:2,flexWrap:'wrap'}}>
   <Box><Typography sx={{fontWeight:800}}>Institute roles</Typography><Typography variant="body2" color="text.secondary">Built-in roles are protected. Custom roles cannot receive platform, user-management or institute-settings privileges.</Typography></Box>
   <Button variant="contained" onClick={()=>start()}>Create custom role</Button>
  </Box>
  <Stack spacing={1.5}>{roles.map(role=><Card key={role.id} variant="outlined"><CardContent sx={{display:'flex',alignItems:'center',justifyContent:'space-between',gap:2,flexWrap:'wrap'}}>
   <Box sx={{minWidth:0}}><Stack direction="row" spacing={1} alignItems="center"><Typography fontWeight={800}>{role.name}</Typography><Chip size="small" label={role.system_role?'Built-in':'Custom'} color={role.system_role?'default':'primary'}/></Stack><Typography variant="caption" color="text.secondary">{role.code} · {role.permission_count} permissions</Typography>{role.description&&<Typography variant="body2" color="text.secondary">{role.description}</Typography>}</Box>
   {!role.system_role&&<Stack direction="row" spacing={1}><Button size="small" onClick={()=>start(role)}>Edit</Button><Button size="small" color="error" onClick={()=>remove(role)}>Delete</Button></Stack>}
  </CardContent></Card>)}</Stack>
  <Dialog open={open} onClose={()=>!busy&&setOpen(false)} fullWidth maxWidth="md">
   <DialogTitle>{editing?'Edit custom role':'Create custom role'}</DialogTitle>
   <DialogContent><Stack spacing={2} sx={{pt:1}}>
    <TextField label="Role name" value={name} onChange={e=>setName(e.target.value)} required inputProps={{maxLength:80}}/>
    <TextField label="Description" value={description} onChange={e=>setDescription(e.target.value)} multiline minRows={2} inputProps={{maxLength:300}}/>
    <Typography fontWeight={800}>Allowed permissions</Typography>
    <Typography variant="caption" color="text.secondary">Select only what this role needs. Restricted administrative and platform permissions are not available here.</Typography>
    <FormGroup sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'1fr 1fr'},gap:.5}}>
     {permissions.map(p=><FormControlLabel key={p.code} control={<Checkbox checked={selected.includes(p.code)} onChange={()=>toggle(p.code)}/>} label={<Box><Typography variant="body2" fontWeight={700}>{p.code}</Typography><Typography variant="caption" color="text.secondary">{p.description}</Typography></Box>}/>)}
    </FormGroup>
   </Stack></DialogContent>
   <DialogActions><Button onClick={()=>setOpen(false)} disabled={busy}>Cancel</Button><Button onClick={save} variant="contained" disabled={busy||!name.trim()}>{busy?'Saving…':'Save role'}</Button></DialogActions>
  </Dialog>
 </Stack>;
}
