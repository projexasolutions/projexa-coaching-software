import {useEffect,useState} from 'react';
import {Alert,Box,Card,CardContent,Chip,Stack,Table,TableBody,TableCell,TableHead,TableRow,Typography} from '@mui/material';
import {AutoAwesomeRounded,WarningAmberRounded} from '@mui/icons-material';
import {api} from '../services';

function unwrap(r:any){return r?.data?.data??r?.data??[]}
function err(e:any,f:string){return e?.response?.data?.message||e?.response?.data?.error?.message||f}

export default function IntelligencePage(){
 const [overview,setOverview]=useState<any>(),[risk,setRisk]=useState<any[]>([]),[error,setError]=useState('');
 async function load(){try{setError('');const [o,r]=await Promise.all([api.get('/intelligence/overview'),api.get('/intelligence/risk-students')]);setOverview(unwrap(o));setRisk(unwrap(r))}catch(e:any){setError(err(e,'Unable to load intelligence'))}}
 useEffect(()=>{load()},[]);
 return <Box>
  <Stack direction="row" spacing={1} sx={{mb:2,alignItems:'center'}}><AutoAwesomeRounded color="primary"/><Box><Typography sx={{fontSize:13,color:'#64748b'}}>Operational intelligence that turns attendance, fee and performance signals into actionable priorities.</Typography></Box></Stack>
  {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
  {overview&&<Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',md:'repeat(6,1fr)'},gap:2,mb:2}}>
   {[['Attendance %',Number(overview.attendance.attendance_percentage||0).toFixed(1)],['Active students',overview.attendance.students],['Overdue invoices',overview.fees.overdue_invoices],['Overdue amount','₹'+Number(overview.fees.overdue_amount||0).toLocaleString('en-IN')],['Published results',overview.exams.published_results],['90-day avg %',Number(overview.exams.average_percentage||0).toFixed(1)]].map(([a,b])=><Card key={String(a)} variant="outlined" sx={{borderRadius:3}}><CardContent><Typography sx={{fontSize:10,color:'#64748b'}}>{a}</Typography><Typography sx={{fontSize:22,fontWeight:900,mt:.5}}>{b}</Typography></CardContent></Card>)}</Box>}
  <Card variant="outlined" sx={{borderRadius:3}}><CardContent><Stack direction="row" spacing={1} sx={{alignItems:'center',mb:1}}><WarningAmberRounded color="warning"/><Typography sx={{fontWeight:900}}>Students needing attention</Typography><Chip size="small" label={risk.length}/></Stack><Typography sx={{fontSize:12,color:'#64748b',mb:2}}>Risk score combines recent attendance, overdue fees and published performance. Use this as an operational prioritization signal, not an automated decision.</Typography>
   {!risk.length?<Typography sx={{py:4,textAlign:'center',color:'#64748b'}}>No active students found.</Typography>:<Table size="small"><TableHead><TableRow><TableCell>Student</TableCell><TableCell>Attendance</TableCell><TableCell>Fees</TableCell><TableCell>Performance</TableCell><TableCell>Risk</TableCell></TableRow></TableHead><TableBody>{risk.map(s=><TableRow key={s.student_id}><TableCell>{s.first_name} {s.last_name||''}<Typography sx={{fontSize:11,color:'#64748b'}}>{s.admission_number}</Typography></TableCell><TableCell>{Number(s.attendance_percentage||0).toFixed(1)}%</TableCell><TableCell>{s.overdue_count} · ₹{Number(s.overdue_amount||0).toLocaleString('en-IN')}</TableCell><TableCell>{Number(s.average_percentage||0).toFixed(1)}%</TableCell><TableCell><Chip size="small" label={s.risk_score>=6?'High':s.risk_score>=3?'Medium':'Low'} color={s.risk_score>=6?'error':s.risk_score>=3?'warning':'success'}/></TableCell></TableRow>)}</TableBody></Table>}
  </CardContent></Card>
 </Box>
}
