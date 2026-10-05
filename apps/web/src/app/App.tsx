import {useState,type ReactNode} from 'react';
import {BrowserRouter,Routes,Route,Navigate,NavLink,useParams} from 'react-router-dom';
import {AppBar,Avatar,Box,Button,Chip,Divider,Drawer,IconButton,List,ListItemButton,ListItemIcon,ListItemText,Menu,MenuItem,Stack,Toolbar,Typography} from '@mui/material';
import {DashboardRounded,PeopleRounded,ChatRounded,SchoolRounded,EventAvailableRounded,CalendarMonthRounded,AssignmentRounded,QuizRounded,AutoAwesomeRounded,AccountBalanceWalletRounded,CampaignRounded,NotificationsRounded,BarChartRounded,SupportAgentRounded,SettingsRounded,LogoutRounded,MenuRounded} from '@mui/icons-material';
import LoginPage from '../features/auth/LoginPage';
import {useAuth} from '../features/auth/authStore';
import {LiveDashboard,LiveStudentsPage,LiveStudentProfile,LiveAcademicsPage,LiveAttendancePage,LiveFinancePage} from '../features/Phase1Pages';
import AcademicsWorkspacePage from '../features/AcademicsWorkspacePage';
import TimetablePage from '../features/TimetablePage';
import FacultyPage from '../features/FacultyPage';
import HomeworkPage from '../features/HomeworkPage';
import ExamsPage from '../features/ExamsPage';
import ResultsPage from '../features/ResultsPage';
import IntelligencePage from '../features/IntelligencePage';
import AdmissionsPage from '../features/AdmissionsPage';
import CommunicationPage from '../features/CommunicationPage';
import PortalPage from '../features/PortalPage';
import ReportsPage from '../features/ReportsPage';
import SupportPage from '../features/SupportPage';
import SettingsPage from '../features/SettingsPage';
import AutomationPage from '../features/AutomationPage';
import QuestionBankPage from '../features/QuestionBankPage';
import NotificationsPage from '../features/NotificationsPage';

const navGroups:any[] = [
  {
    label:'OVERVIEW',
    items:[
      ['/owner','Command Center',DashboardRounded],
      ['/owner/students','Students',PeopleRounded],
      ['/owner/academics','Academics',SchoolRounded],
      ['/owner/attendance','Attendance',EventAvailableRounded],
    ]
  },
  {
    label:'OPERATIONS',
    items:[
      ['/owner/timetable','Timetable',CalendarMonthRounded],
      ['/owner/faculty','Faculty',PeopleRounded],
      ['/owner/homework','Homework',AssignmentRounded],
      ['/owner/exams','Exams & Results',QuizRounded],
      ['/owner/results','Results & Analytics',BarChartRounded],
      ['/owner/finance','Finance',AccountBalanceWalletRounded],
    ]
  },
  {
    label:'GROWTH & ENGAGEMENT',
    items:[
      ['/owner/admissions','Admissions CRM',PeopleRounded],
      ['/owner/communication','Communication',ChatRounded],
      ['/owner/notifications','Notifications',NotificationsRounded],
      ['/portal','Parent / Student Portal',PeopleRounded],
    ]
  },
  {
    label:'SYSTEM',
    items:[
      ['/owner/intelligence','AI Intelligence',AutoAwesomeRounded],
      ['/owner/reports','Reports',BarChartRounded],
      ['/owner/support','Support',SupportAgentRounded],
      ['/owner/settings','Settings',SettingsRounded],
    ]
  }
];

function Shell({children}:{children:ReactNode}){
 const session=useAuth(s=>s.session),logout=useAuth(s=>s.logout);
 const [mobile,setMobile]=useState(false),[anchor,setAnchor]=useState<HTMLElement|null>(null);

 const side=<Box sx={{height:'100%',display:'flex',flexDirection:'column',bgcolor:'#fff'}}>
  <Toolbar sx={{minHeight:'68px !important',px:2.25}}>
   <Box sx={{width:36,height:36,borderRadius:2.25,display:'grid',placeItems:'center',bgcolor:'#0f766e',color:'#fff',fontWeight:900,boxShadow:'0 4px 12px rgba(15,118,110,.18)'}}>P</Box>
   <Box sx={{ml:1.25}}>
    <Typography sx={{fontWeight:950,fontSize:15,letterSpacing:-.4,lineHeight:1}}>PROJEXA</Typography>
    <Typography sx={{fontSize:9.5,color:'#94a3b8',fontWeight:850,letterSpacing:1.3,mt:.45}}>COACHING OS</Typography>
   </Box>
  </Toolbar>
  <Divider sx={{borderColor:'#eef2f7'}}/>
  <Box sx={{p:1.25,flex:1,overflowY:'auto'}}>
   {navGroups.map((group:any)=><Box key={group.label} sx={{mb:1.7}}>
    <Typography sx={{px:1.1,mb:.65,fontSize:9.5,fontWeight:900,color:'#94a3b8',letterSpacing:1.15}}>{group.label}</Typography>
    <List disablePadding>
     {group.items.map(([to,label,Icon]:any)=><ListItemButton key={to} component={NavLink} to={to} onClick={()=>setMobile(false)}
       sx={{
        mb:.25,borderRadius:2,py:.85,px:1.1,color:'#64748b',minHeight:40,
        transition:'all .15s ease',
        '&:hover':{bgcolor:'#f8fafc',color:'#0f766e'},
        '&.active':{bgcolor:'#e8f8f5',color:'#0f766e',fontWeight:800},
        '&.active .MuiListItemIcon-root':{color:'#0f766e'}
       }}>
       <ListItemIcon sx={{minWidth:32,color:'inherit'}}><Icon sx={{fontSize:18}}/></ListItemIcon>
       <ListItemText primary={label} slotProps={{primary:{sx:{fontSize:12.5,fontWeight:700,letterSpacing:-.05}}}}/>
      </ListItemButton>)}
    </List>
   </Box>)}
  </Box>
  <Box sx={{p:1.25}}>
   <Box sx={{p:1.4,bgcolor:'#f8fafc',border:'1px solid #edf0f4',borderRadius:2.5}}>
    <Typography sx={{fontSize:11.5,fontWeight:850}}>Institute workspace</Typography>
    <Typography sx={{fontSize:10.5,color:'#64748b',mt:.4,lineHeight:1.45}}>Tenant-isolated operational system.</Typography>
   </Box>
  </Box>
 </Box>;

 return <Box sx={{display:'flex',minHeight:'100vh',bgcolor:'#f6f8fb'}}>
  {mobile
   ? <Drawer open onClose={()=>setMobile(false)} slotProps={{paper:{sx:{width:280}}}}>{side}</Drawer>
   : <Drawer variant="permanent" slotProps={{paper:{sx:{width:244,borderRight:'1px solid #e8edf3',boxShadow:'none'}}}} sx={{width:244,flexShrink:0}}>{side}</Drawer>}
  <Box sx={{flex:1,minWidth:0}}>
   <AppBar position="sticky" elevation={0} color="inherit" sx={{bgcolor:'rgba(255,255,255,.92)',backdropFilter:'blur(14px)',borderBottom:'1px solid #e8edf3'}}>
    <Toolbar sx={{minHeight:'64px !important',px:{xs:1.5,md:2.5},gap:1}}>
     <IconButton onClick={()=>setMobile(true)} sx={{display:{md:'none'}}}><MenuRounded/></IconButton>
     <Box sx={{flex:1,minWidth:0}}>
      <Typography noWrap sx={{fontWeight:850,fontSize:13.5,color:'#0f172a'}}>{session?.tenantName||'Institute'}</Typography>
      <Typography noWrap sx={{fontSize:10.5,color:'#94a3b8'}}>Academic workspace</Typography>
     </Box>
     <Button size="small" variant="outlined" sx={{display:{xs:'none',sm:'inline-flex'},borderColor:'#e2e8f0',color:'#475569',borderRadius:2,textTransform:'none',fontWeight:700}}>Help</Button>
     <IconButton onClick={e=>setAnchor(e.currentTarget)} sx={{p:.35}}>
      <Avatar sx={{width:34,height:34,bgcolor:'#0f766e',fontSize:12,fontWeight:800}}>OP</Avatar>
     </IconButton>
     <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={()=>setAnchor(null)} PaperProps={{sx:{mt:1,borderRadius:2.5,minWidth:150}}}>
      <MenuItem onClick={logout}><LogoutRounded fontSize="small"/><Box component="span" sx={{ml:1}}>Sign out</Box></MenuItem>
     </Menu>
    </Toolbar>
   </AppBar>
   {children}
  </Box>
 </Box>;
}
function Page({title,subtitle,children}:{title:string;subtitle:string;children:ReactNode}){
 return <Box sx={{px:{xs:2,sm:2.5,md:3.5,lg:4.5},py:{xs:2.5,md:3.5},maxWidth:1500,mx:'auto'}}>
  <Box sx={{mb:{xs:2.5,md:3.25}}}>
   <Typography component="h1" sx={{fontSize:{xs:25,sm:28,md:31},fontWeight:950,letterSpacing:-1.15,lineHeight:1.1,color:'#0f172a'}}>{title}</Typography>
   <Typography sx={{fontSize:13.5,color:'#64748b',mt:.7}}>{subtitle}</Typography>
  </Box>
  {children}
 </Box>;
}

function Guard({children}:{children:ReactNode}){return useAuth(s=>s.session)?children:<Navigate to="/login" replace/>}
function Profile(){const {id}=useParams();return <LiveStudentProfile id={id||''}/>}

function Placeholder({title}:{title:string}){return <Box sx={{p:5,textAlign:'center',bgcolor:'#fff',border:'1px solid #e5e7eb',borderRadius:3}}><Typography sx={{fontSize:22,fontWeight:900}}>{title}</Typography><Typography sx={{color:'#64748b',mt:1}}>This module is scheduled for the next implementation stage. Phase 1 operational modules are live.</Typography><Chip label="Phase 1 in progress" sx={{mt:2}}/></Box>}

export default function App(){return <BrowserRouter><Routes>
 <Route path="/login" element={<LoginPage/>}/>
 <Route path="/owner" element={<Guard><Shell><Page title="Command Center" subtitle="Live operational view of your institute."><LiveDashboard/></Page></Shell></Guard>}/>
 <Route path="/owner/students" element={<Guard><Shell><Page title="Students" subtitle="Manage student records and operational data."><LiveStudentsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/students/:id" element={<Guard><Shell><Page title="Student Profile" subtitle="Live student record and fee ledger."><Profile/></Page></Shell></Guard>}/>
 <Route path="/owner/academics" element={<Guard><Shell><Page title="Academics" subtitle="Configure academic years, classes, streams, subjects and batches."><AcademicsWorkspacePage/></Page></Shell></Guard>}/>
 <Route path="/owner/attendance" element={<Guard><Shell><Page title="Attendance" subtitle="Create sessions and mark live attendance."><LiveAttendancePage/></Page></Shell></Guard>}/>
 <Route path="/owner/finance" element={<Guard><Shell><Page title="Finance" subtitle="Manage invoices, collections and outstanding fees."><LiveFinancePage/></Page></Shell></Guard>}/>
 <Route path="/owner/timetable" element={<Guard><Shell><Page title="Timetable Management" subtitle="Schedule batches, teachers and classrooms with conflict protection."><TimetablePage/></Page></Shell></Guard>}/>
 <Route path="/owner/faculty" element={<Guard><Shell><Page title="Faculty Management" subtitle="Manage teachers, teaching assignments and classroom resources."><FacultyPage/></Page></Shell></Guard>}/>
 <Route path="/owner/homework" element={<Guard><Shell><Page title="Teaching & Learning" subtitle="Create homework and track student submissions."><HomeworkPage/></Page></Shell></Guard>}/>
 <Route path="/owner/exams" element={<Guard><Shell><Page title="Exams & Results" subtitle="Build exams, configure subjects and manage question sets."><ExamsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/results" element={<Guard><Shell><Page title="Results & Analytics" subtitle="Generate, publish and analyze examination performance."><ResultsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/intelligence" element={<Guard><Shell><Page title="AI Intelligence" subtitle="Prioritize institute risks and learning opportunities from operational data."><IntelligencePage/></Page></Shell></Guard>}/>
 <Route path="/portal" element={<Guard><Shell><Page title="Parent / Student Portal" subtitle="Attendance, fees, results and homework in one workspace."><PortalPage/></Page></Shell></Guard>}/>
 <Route path="/owner/communication" element={<Guard><Shell><Page title="Communication" subtitle="Internal conversations and institute communication workspace."><CommunicationPage/></Page></Shell></Guard>}/>
 <Route path="/owner/admissions" element={<Guard><Shell><Page title="Admissions CRM" subtitle="Manage enquiries, counselling, demos, follow-ups and conversions."><AdmissionsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/reports" element={<Guard><Shell><Page title="Reports" subtitle="Institute performance, collections and attendance reporting."><ReportsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/support" element={<Guard><Shell><Page title="Support" subtitle="Track institute support requests and operational issues."><SupportPage/></Page></Shell></Guard>}/>
 <Route path="/owner/settings" element={<Guard><Shell><Page title="Settings" subtitle="Configure institute-level operational policies."><SettingsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/automation" element={<Guard><Shell><Page title="Automation Center" subtitle="Create controlled event-driven workflows for repetitive operations."><AutomationPage/></Page></Shell></Guard>}/>
 <Route path="/owner/notifications" element={<Guard><Shell><Page title="Notifications" subtitle="System alerts and workflow notifications."><NotificationsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/questions" element={<Guard><Shell><Page title="Question Bank" subtitle="Build reusable objective questions for the universal exam engine."><QuestionBankPage/></Page></Shell></Guard>}/>
 <Route path="*" element={<Navigate to="/owner" replace/>}/>
 </Routes></BrowserRouter>}
