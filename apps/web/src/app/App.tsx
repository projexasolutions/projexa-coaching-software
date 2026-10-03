import { useState, type ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink } from 'react-router-dom';
import LoginPage from '../features/auth/LoginPage';
import { useAuth } from '../features/auth/authStore';
import {
  AppBar, Avatar, Badge, Box, Button, Card, CardContent, Chip, Divider, Drawer,
  IconButton, List, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem,
  Paper, Stack, Toolbar, Typography, useMediaQuery, useTheme
} from '@mui/material';
import {
  DashboardRounded, PeopleRounded, SchoolRounded, EventAvailableRounded, CalendarMonthRounded,
  QuizRounded, AccountBalanceWalletRounded, CampaignRounded, AutoAwesomeRounded, BarChartRounded,
  SupportAgentRounded, SettingsRounded, NotificationsRounded, SearchRounded, MenuRounded,
  LogoutRounded, TrendingUpRounded, WarningAmberRounded, CheckCircleRounded, ArrowForwardRounded,
  GroupsRounded, PaymentsRounded, AssignmentTurnedInRounded, AccessTimeRounded
} from '@mui/icons-material';
import ModuleDataPage from '../features/ModuleDataPage';
import {StudentsPage, StudentProfile, AttendancePage, TimetablePage, ExamsPage, FinancePage, AdmissionsPage, AutomationBuilder, CommunicationPage} from '../features/WorkflowPages';
import {LiveDashboard, LiveStudentsPage, LiveStudentProfile, LiveAcademicsPage, LiveAttendancePage, LiveFinancePage} from '../features/Phase1Pages';
import {StudentPortal, ParentPortal, TeacherPortal, SettingsPage} from '../features/PortalPages';

const nav = [
  ['/owner','Command Center',DashboardRounded], ['/owner/students','Students',PeopleRounded],
  ['/owner/academics','Academics',SchoolRounded], ['/owner/attendance','Attendance',EventAvailableRounded],
  ['/owner/timetable','Timetable',CalendarMonthRounded], ['/owner/exams','Exams & Results',QuizRounded],
  ['/owner/finance','Finance',AccountBalanceWalletRounded], ['/owner/admissions','Admissions',CampaignRounded],
  ['/owner/communication','Communication',NotificationsRounded], ['/owner/automations','Automations',AutoAwesomeRounded],
  ['/owner/reports','Reports & Analytics',BarChartRounded], ['/owner/support','Support',SupportAgentRounded],
  ['/owner/settings','Settings',SettingsRounded]
] as const;

const stats = [
  {label:'Active Students', value:'1,248', delta:'+8.4%', icon:PeopleRounded, tone:'teal'},
  {label:'Today Attendance', value:'91.6%', delta:'+2.1%', icon:EventAvailableRounded, tone:'blue'},
  {label:'Fee Collection', value:'₹8.42L', delta:'+12.8%', icon:PaymentsRounded, tone:'green'},
  {label:'Open Attention', value:'27', delta:'9 urgent', icon:WarningAmberRounded, tone:'amber'}
];

function Shell({children}:{children:ReactNode}) {
  const session=useAuth(s=>s.session); const logout=useAuth(s=>s.logout); const theme=useTheme();
  const mobile=useMediaQuery(theme.breakpoints.down('md')); const [open,setOpen]=useState(false); const [anchor,setAnchor]=useState<null|HTMLElement>(null);
  const sidebar=<Box sx={{height:'100%',display:'flex',flexDirection:'column'}}>
    <Toolbar sx={{px:2.5,gap:1.2,minHeight:'72px !important'}}>
      <Box sx={{width:38,height:38,borderRadius:2.2,display:'grid',placeItems:'center',background:'linear-gradient(135deg,#0f766e,#14b8a6)',color:'#fff',fontWeight:900,fontSize:18}}>P</Box>
      <Box><Typography fontWeight={900} letterSpacing={-.5}>PROJEXA</Typography><Typography sx={{fontSize:10,color:'#94a3b8',fontWeight:700,letterSpacing:1.2}}>COACHING OS</Typography></Box>
    </Toolbar>
    <Divider/>
    <Box sx={{px:1.5,pt:2,overflowY:'auto',flex:1}}><Typography sx={{px:1.5,mb:1,fontSize:10,fontWeight:800,color:'#94a3b8',letterSpacing:1.2}}>WORKSPACE</Typography>
      <List disablePadding>{nav.map(([to,label,Icon])=><ListItemButton key={to} component={NavLink} to={to} onClick={()=>mobile&&setOpen(false)} sx={{mb:.45,px:1.4,py:1.05,borderRadius:2, color:'#64748b','& .MuiListItemIcon-root':{minWidth:34,color:'inherit'},'&.active':{background:'#e8f8f5',color:'#0f766e',fontWeight:800},'&:hover':{background:'#f1f5f9'}}}><ListItemIcon><Icon fontSize="small"/></ListItemIcon><ListItemText primary={label} primaryTypographyProps={{fontSize:13,fontWeight:600}}/></ListItemButton>)}</List>
    </Box>
    <Box sx={{p:1.5}}><Paper elevation={0} sx={{p:1.5,bgcolor:'#f8fafc',border:'1px solid #e2e8f0'}}><Typography fontSize={11} fontWeight={800}>Need help?</Typography><Typography fontSize={11} color="text.secondary" sx={{mb:1}}>Contact Projexa support.</Typography><Button size="small" endIcon={<ArrowForwardRounded/>} sx={{textTransform:'none',p:0}}>Open support</Button></Paper></Box>
  </Box>;
  return <Box sx={{display:'flex',minHeight:'100vh',bgcolor:'#f7f9fc'}}>
    {mobile?<Drawer open={open} onClose={()=>setOpen(false)} PaperProps={{sx:{width:272}}}>{sidebar}</Drawer>:<Drawer variant="permanent" PaperProps={{sx:{width:260,borderRight:'1px solid #e5e7eb'}}} sx={{width:260,flexShrink:0}}>{sidebar}</Drawer>}
    <Box sx={{flex:1,minWidth:0}}><AppBar position="sticky" elevation={0} color="inherit" sx={{bgcolor:'rgba(255,255,255,.92)',backdropFilter:'blur(10px)',borderBottom:'1px solid #e5e7eb'}}><Toolbar sx={{gap:1}}>
      {mobile&&<IconButton onClick={()=>setOpen(true)}><MenuRounded/></IconButton>}<Box sx={{flex:1}}><Typography fontWeight={800} fontSize={14}>{session?.tenantName || 'Institute'}</Typography><Typography fontSize={11} color="text.secondary">Academic workspace</Typography></Box>
      <IconButton><SearchRounded fontSize="small"/></IconButton><IconButton><Badge badgeContent={4} color="error"><NotificationsRounded fontSize="small"/></Badge></IconButton>
      <IconButton onClick={e=>setAnchor(e.currentTarget)}><Avatar sx={{width:34,height:34,bgcolor:'#0f766e',fontSize:13,fontWeight:800}}>OP</Avatar></IconButton>
      <Menu anchorEl={anchor} open={!!anchor} onClose={()=>setAnchor(null)}><MenuItem onClick={logout}><ListItemIcon><LogoutRounded fontSize="small"/></ListItemIcon>Sign out</MenuItem></Menu>
    </Toolbar></AppBar>{children}</Box>
  </Box>;
}

function Page({children,title,subtitle,actions}:{children:ReactNode;title:string;subtitle?:string;actions?:ReactNode}){return <Box sx={{p:{xs:2,sm:3,lg:4},maxWidth:1600,mx:'auto'}}><Stack direction={{xs:'column',sm:'row'}} justifyContent="space-between" alignItems={{sm:'center'}} gap={2} mb={3}><Box><Typography sx={{fontSize:{xs:25,sm:30},fontWeight:900,letterSpacing:-1}}>{title}</Typography>{subtitle&&<Typography color="text.secondary" fontSize={14}>{subtitle}</Typography>}</Box>{actions}</Stack>{children}</Box>}

function StatCard({item}:{item:any}){const Icon=item.icon;return <Card elevation={0} sx={{height:'100%',border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:2.3}}><Stack direction="row" justifyContent="space-between"><Box sx={{width:40,height:40,borderRadius:2,display:'grid',placeItems:'center',bgcolor:item.tone==='amber'?'#fff7ed':'#ecfdf5',color:item.tone==='blue'?'#2563eb':item.tone==='amber'?'#d97706':'#0f766e'}}><Icon fontSize="small"/></Box><Chip size="small" label={item.delta} sx={{height:24,bgcolor:item.tone==='amber'?'#fff7ed':'#ecfdf5',color:item.tone==='amber'?'#b45309':'#047857',fontWeight:800}}/></Stack><Typography color="text.secondary" fontSize={12} mt={2}>{item.label}</Typography><Typography fontSize={28} fontWeight={900} letterSpacing={-.7}>{item.value}</Typography></CardContent></Card>}

function Dashboard(){return <Shell><Page title="Good morning, Owner" subtitle="Here’s what needs your attention across the institute today." actions={<Stack direction="row" gap={1}><Button variant="outlined" href="/portal/teacher" sx={{textTransform:'none',fontWeight:800}}>Teacher view</Button><Button variant="contained" startIcon={<AssignmentTurnedInRounded/>} sx={{textTransform:'none',fontWeight:800,borderRadius:2.2,boxShadow:'none'}}>View today</Button></Stack>}>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'repeat(2,1fr)',lg:'repeat(4,1fr)'},gap:2,mb:3}}>{stats.map(s=><StatCard key={s.label} item={s}/>)}</Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'1.45fr 1fr'},gap:2,mb:2}}>
    <Card elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:2.5}}><Stack direction="row" justifyContent="space-between" mb={2}><Box><Typography fontWeight={900}>Attendance overview</Typography><Typography fontSize={12} color="text.secondary">Last 7 academic days</Typography></Box><Chip label="91.6% avg" size="small"/></Stack><Box sx={{height:210,display:'flex',alignItems:'end',gap:{xs:1,sm:2},px:1}}>{[72,81,76,88,92,87,94].map((v,i)=><Box key={i} sx={{flex:1,textAlign:'center'}}><Box sx={{height:170,display:'flex',alignItems:'end'}}><Box sx={{height:`${v}%`,width:'100%',maxWidth:42,mx:'auto',borderRadius:'7px 7px 2px 2px',background:'linear-gradient(180deg,#14b8a6,#0f766e)'}}/></Box><Typography fontSize={10} color="text.secondary" mt={1}>{['Mon','Tue','Wed','Thu','Fri','Sat','Today'][i]}</Typography></Box>)}</Box></CardContent></Card>
    <Card elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:2.5}}><Typography fontWeight={900}>Needs attention</Typography><Typography fontSize={12} color="text.secondary" mb={2}>Prioritized automatically</Typography>{[['9 students','Attendance below 75%',WarningAmberRounded,'#d97706'],['₹1.28L','Fees overdue',PaymentsRounded,'#dc2626'],['4 batches','Performance declining',TrendingUpRounded,'#7c3aed'],['3 tickets','Need response today',SupportAgentRounded,'#2563eb']].map(([v,l,I,c])=>{const Icon=I as any;return <Stack key={l as string} direction="row" alignItems="center" gap={1.5} sx={{p:1.2,borderBottom:'1px solid #f1f5f9'}}><Box sx={{width:34,height:34,borderRadius:2,bgcolor:`${c}12`,color:c,display:'grid',placeItems:'center'}}><Icon fontSize="small"/></Box><Box flex={1}><Typography fontSize={13} fontWeight={800}>{v as string}</Typography><Typography fontSize={11} color="text.secondary">{l as string}</Typography></Box><IconButton size="small"><ArrowForwardRounded fontSize="small"/></IconButton></Stack>})}</CardContent></Card>
  </Box>
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'1fr 1fr'},gap:2}}>
    <Card elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:2.5}}><Stack direction="row" justifyContent="space-between" mb={2}><Typography fontWeight={900}>Today’s classes</Typography><Button size="small" sx={{textTransform:'none'}}>View timetable</Button></Stack>{[['09:00','12-A • Physics','Rahul Sharma'],['10:30','11-B • Mathematics','Priya Mehta'],['12:00','10-A • English','Amit Joshi'],['14:00','12-C • Chemistry','Neha Patil']].map(r=><Stack key={r[0]} direction="row" alignItems="center" gap={2} py={1.15} borderBottom="1px solid #f1f5f9"><Typography fontSize={12} fontWeight={900} sx={{width:48}}>{r[0]}</Typography><Box flex={1}><Typography fontSize={13} fontWeight={700}>{r[1]}</Typography><Typography fontSize={11} color="text.secondary">{r[2]}</Typography></Box><Chip label="Scheduled" size="small" variant="outlined"/></Stack>)}</CardContent></Card>
    <Card elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:2.5}}><Typography fontWeight={900} mb={2}>Recent activity</Typography>{[['Payment received','INV-1048 • ₹12,500','2 min ago'],['Result published','Unit Test 3 • Class 12-A','18 min ago'],['New admission','Aarav Kulkarni • 11 PCM','42 min ago'],['Automation completed','Low attendance reminder','1 hr ago']].map(r=><Stack key={r[0]} direction="row" gap={1.5} py={1.15} borderBottom="1px solid #f1f5f9"><CheckCircleRounded sx={{fontSize:18,color:'#0f766e',mt:.2}}/><Box flex={1}><Typography fontSize={13} fontWeight={700}>{r[0]}</Typography><Typography fontSize={11} color="text.secondary">{r[1]}</Typography></Box><Typography fontSize={10} color="text.secondary">{r[2]}</Typography></Stack>)}</CardContent></Card>
  </Box>
</Page></Shell>}

const moduleConfig:Record<string,{title:string;subtitle:string;endpoint?:string;columns?:string[]}>={
 students:{title:'Students',subtitle:'Manage student records, enrollment and academic history.',endpoint:'/students',columns:['admissionNumber','firstName','lastName','phone','status']},
 academics:{title:'Academic Setup',subtitle:'Configure years, classes, streams, subjects and batches.'},
 attendance:{title:'Attendance',subtitle:'Monitor attendance, interventions and daily sessions.'},
 timetable:{title:'Timetable',subtitle:'Plan classes while preventing teacher and room conflicts.'},
 exams:{title:'Exams & Results',subtitle:'Build assessments, publish results and track performance.'},
 finance:{title:'Finance',subtitle:'Track invoices, collections, outstanding fees and receipts.',endpoint:'/finance/invoices',columns:['invoice_number','amount','paid_amount','due_date','status']},
 admissions:{title:'Admissions CRM',subtitle:'Move enquiries from first contact to enrolled student.',endpoint:'/admissions/leads',columns:['name','phone','source','stage']},
 communication:{title:'Communication',subtitle:'Keep students, parents and teachers connected.'},
 automations:{title:'Automation Center',subtitle:'Turn institute events into controlled workflows.',endpoint:'/automations',columns:['name','trigger_event','active','cooldown_seconds']},
 reports:{title:'Reports & Analytics',subtitle:'Understand attendance, finance and academic performance.',endpoint:'/reports/attendance',columns:['admission_number','first_name','present','total']},
 support:{title:'Support Desk',subtitle:'Track complaints, requests and escalations.'},
 settings:{title:'Institute Settings',subtitle:'Configure branding, policies, permissions and integrations.'}
};
function Module({kind}:{kind:string}){const c=moduleConfig[kind]; const workflow:any={students:<LiveStudentsPage/>,academics:<LiveAcademicsPage/>,attendance:<LiveAttendancePage/>,timetable:<TimetablePage/>,exams:<ExamsPage/>,finance:<LiveFinancePage/>,admissions:<AdmissionsPage/>,communication:<CommunicationPage/>,automations:<AutomationBuilder/>,reports:<ModuleDataPage title="Reports & Analytics" endpoint="/reports/attendance" columns={['admission_number','first_name','present','total']}/>,support:<GenericModule kind="support"/>,settings:<SettingsPage/>}; return <Shell><Page title={c.title} subtitle={c.subtitle}>{workflow[kind]??<GenericModule kind={kind}/>}</Page></Shell>}
function GenericModule({kind}:{kind:string}){const cards:Record<string,[string,string,any][]>={academics:[['Academic Years','2026–27 active','2'],['Classes','5th–12th enabled','8'],['Subjects','42 configured','42'],['Batches','31 active batches','31']],attendance:[['Present today','1,143','91.6%'],['Absent','105','8.4%'],['Low attendance','74','Needs action'],['Sessions','48 today','100%']],timetable:[['Classes today','48','Scheduled'],['Teacher conflicts','0','Clear'],['Room conflicts','2','Review'],['Substitutions','3','Assigned']],exams:[['Upcoming exams','6','This month'],['Published results','18','This term'],['Question bank','1,284','Questions'],['Avg score','72.4%','+4.8%']],communication:[['Unread messages','38','Across institute'],['Announcements','12','This month'],['Parent conversations','16','Open'],['Video sessions','5','Today']],support:[['Open tickets','14','3 urgent'],['Avg response','2h 18m','-24%'],['Escalations','3','Today'],['Resolved','86','This month']],settings:[['Branding','Configured','Ready'],['Roles','6 roles','RBAC'],['Integrations','4 connected','Healthy'],['Policies','12 active','Configured']]}[kind]??[];return <><Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr 1fr',md:'repeat(4,1fr)'},gap:2,mb:3}}>{cards.map(([a,b,d],i)=><Card key={a} elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent><Typography fontSize={12} color="text.secondary">{a}</Typography><Typography fontSize={24} fontWeight={900}>{b}</Typography><Chip size="small" label={d} sx={{mt:1}}/></CardContent></Card>)}</Box><Card elevation={0} sx={{border:'1px solid #e5e7eb',borderRadius:3}}><CardContent sx={{p:3}}><Stack direction="row" justifyContent="space-between" mb={2}><Box><Typography fontWeight={900}>Workspace overview</Typography><Typography fontSize={12} color="text.secondary">Use the controls below to manage this area.</Typography></Box><Chip label="Live" color="success" variant="outlined"/></Stack><Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'repeat(3,1fr)'},gap:1.5}}>{['Overview','Activity','Configuration'].map((x,i)=><Paper key={x} variant="outlined" sx={{p:2,borderRadius:2}}><Typography fontWeight={800} fontSize={13}>{x}</Typography><Typography fontSize={11} color="text.secondary" mt={.5}>{i===0?'Monitor current institute activity and key metrics.':i===1?'Review recent changes and operational events.':'Configure policies and workflow behavior.'}</Typography><Button size="small" endIcon={<ArrowForwardRounded/>} sx={{mt:1,textTransform:'none',p:0}}>Open</Button></Paper>)}</Box></CardContent></Card></>}
function Guard({children}:{children:ReactNode}){return useAuth(s=>s.session)?children:<Navigate to="/login" replace/>}
export default function App(){return <BrowserRouter><Routes><Route path="/login" element={<LoginPage/>}/><Route path="/owner" element={<Guard><Shell><Page title="Command Center" subtitle="Live operational view of your institute."><LiveDashboard/></Page></Shell></Guard>}/>{Object.keys(moduleConfig).map(k=><Route key={k} path={`/owner/${k}`} element={<Guard><Module kind={k}/></Guard>}/>) }<Route path="/owner/students/:id" element={<Guard><Shell><Page title="Student Profile" subtitle="360° student record, performance, attendance and fees."><StudentProfile/></Page></Shell></Guard>}/><Route path="/portal/student" element={<Guard><Shell><Page title="Student Portal" subtitle="Your classes, learning, attendance, results and fees."><StudentPortal/></Page></Shell></Guard>}/><Route path="/portal/parent" element={<Guard><Shell><Page title="Parent Portal" subtitle="Stay on top of your child’s attendance, results, fees and communication."><ParentPortal/></Page></Shell></Guard>}/><Route path="/portal/teacher" element={<Guard><Shell><Page title="Teacher Workspace" subtitle="Manage classes, attendance, reviews and parent communication."><TeacherPortal/></Page></Shell></Guard>}/><Route path="*" element={<Navigate to="/owner" replace/>}/></Routes></BrowserRouter>}
