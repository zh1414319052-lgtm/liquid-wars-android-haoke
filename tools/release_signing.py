"""Windows-only independent release identity and signing. GPL-3.0-or-later.
Never accepts passwords as arguments. init uses a local masked GUI; sign uses DPAPI.
Private keys and protected credentials stay outside source and deliverables.
"""
from pathlib import Path
import argparse,base64,ctypes,hashlib,json,os,re,subprocess,sys,threading
from ctypes import wintypes

ROOT=Path(__file__).resolve().parents[1]
APP_ID='com.xenris.liquidwarsos.haoke'
ALIAS='liquidwars-haoke-release'
SIGNROOT=Path(os.environ.get('LOCALAPPDATA',Path.home()/'AppData/Local'))/'Haoke/AndroidSigning'
IDENTITY=SIGNROOT/'liquidwars-release'
PUBLIC=ROOT/'docs/game/formal-1.53/signing-public.json'
STATUS=ROOT/'build/formal-signing-status.json'

class SigningError(Exception):pass
def jsonfile(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def status(state,**kw):jsonfile(STATUS,dict(state=state,**kw))
def protected(data,encrypt):
    class B(ctypes.Structure):_fields_=[('n',wintypes.DWORD),('p',ctypes.POINTER(ctypes.c_ubyte))]
    buf=(ctypes.c_ubyte*len(data)).from_buffer_copy(data);a=B(len(data),buf);b=B()
    dll=ctypes.WinDLL('crypt32',use_last_error=True)
    fn=dll.CryptProtectData if encrypt else dll.CryptUnprotectData
    fn.argtypes=[ctypes.POINTER(B),ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,wintypes.DWORD,ctypes.POINTER(B)]
    fn.restype=wintypes.BOOL
    if not fn(ctypes.byref(a),None,None,None,None,1,ctypes.byref(b)):raise SigningError('dpapi_unavailable')
    kernel=ctypes.WinDLL('kernel32');kernel.LocalFree.argtypes=[ctypes.c_void_p]
    try:return ctypes.string_at(b.p,b.n)
    finally:kernel.LocalFree(b.p)
def pack(obj):return base64.b64encode(protected(json.dumps(obj).encode('utf8'),True))
def unpack(p):return json.loads(protected(base64.b64decode(p.read_bytes(),validate=True),False))
def run(args,env=None):
    p=subprocess.run([str(v) for v in args],env=env,capture_output=True,timeout=120)
    if p.returncode:raise SigningError('tool_failed_'+Path(str(args[0])).stem)
    return p.stdout
def keytool():
    p=Path(os.environ.get('JAVA_HOME',''))/'bin/keytool.exe'
    if not p.is_file():raise SigningError('java_home_missing')
    return p
def private_dir(p):
    if not p.resolve().is_relative_to(SIGNROOT.resolve()) or p.is_symlink():raise SigningError('unsafe_identity_path')
    p.mkdir(parents=True,exist_ok=False)
    # Only restrict a newly created, task-owned directory; never change an old identity.
    run(['icacls.exe',p,'/inheritance:r','/grant:r',os.environ['USERNAME']+':(OI)(CI)F','*S-1-5-18:(OI)(CI)F'])
def certificate(store,alias,env):return run([keytool(),'-exportcert','-keystore',store,'-alias',alias,'-storepass:env','LW_SIGNING_STORE_PASS'],env)
def sha(data):return hashlib.sha256(data).hexdigest().upper()
def source_identity():
    # The only approved password source; no scan of unrelated credentials.
    p=Path.home()/'.haoke/AndroidSigning/anxreader-release/signing-identity.json'
    if not p.is_file():raise SigningError('approved_source_identity_missing')
    return p.parent,json.loads(p.read_text('utf8'))
def initialize_saved():
    shared=SIGNROOT/'shared-release-password/credentials.dpapi'
    secret=None
    if shared.is_file():
        values=unpack(shared);secret=values['store_pass'];values.clear()
    else:
        # User explicitly authorized discovery/reuse on 2026-11-06. Validate BOTH
        # registered certificates; borrow only the password, never another private key.
        directory=Path.home()/'.haoke/AndroidSigning/apk-extractor-release'
        meta=json.loads((directory/'signing-identity.json').read_text('utf8'))
        pin='2e6e05a950d7af16e0134ca0e7967ecb727a421c0f3eb999d0ecaa48400001bb'
        if meta['applicationId']!='axp.tool.apkextractor' or meta['certificateSha256'].lower()!=pin:raise SigningError('saved_source_metadata_mismatch')
        credential=Path(meta['credentialsRef']);store=Path(meta['keystoreFile'])
        if credential.resolve().parent!=directory.resolve() or store.resolve().parent!=directory.resolve():raise SigningError('saved_source_path_mismatch')
        values=unpack(credential);secret=values['store_pass'];values.clear()
        env={**os.environ,'LW_SIGNING_STORE_PASS':secret}
        try:
            if sha(certificate(store,meta['alias'],env)).lower()!=pin:raise SigningError('saved_source_certificate_mismatch')
        finally:env.pop('LW_SIGNING_STORE_PASS',None)
    try:
        result=initialize(secret) # Independently checks the approved Anx certificate.
        print(json.dumps(dict(status='READY',applicationId=APP_ID,certificateSha256=result['certificateSha256'],
                              passwordReuse='Verified registered Anx password; independent Liquid Wars key')))
    finally:secret=None
def initialize(secret):
    env={**os.environ,'LW_SIGNING_STORE_PASS':secret,'LW_SIGNING_KEY_PASS':secret}
    try:
        source,meta=source_identity()
        try:der=certificate(source/meta['keystoreFile'],meta['alias'],env)
        except SigningError:raise SigningError('shared_password_check_failed') from None
        if sha(der)!=meta['certificateSha256'].upper():raise SigningError('approved_source_certificate_mismatch')
        if IDENTITY.exists():raise SigningError('existing_identity_preserved')
        private_dir(IDENTITY)
        store=IDENTITY/'liquidwars-haoke-release.p12'
        run([keytool(),'-genkeypair','-keystore',store,'-storetype','PKCS12','-alias',ALIAS,
             '-keyalg','RSA','-keysize','4096','-validity','18262','-dname','CN=Haoke Liquid Wars, OU=Android, O=Haoke, C=CN',
             '-storepass:env','LW_SIGNING_STORE_PASS','-keypass:env','LW_SIGNING_KEY_PASS','-noprompt'],env)
        own=certificate(store,ALIAS,env);fingerprint=sha(own)
        credentials=dict(store_pass=secret,key_pass=secret)
        (IDENTITY/'credentials.dpapi').write_bytes(pack(credentials))
        if unpack(IDENTITY/'credentials.dpapi')!=credentials:raise SigningError('credential_roundtrip_failed')
        (IDENTITY/'release-certificate.cer').write_bytes(own)
        public=dict(applicationId=APP_ID,alias=ALIAS,storeType='PKCS12',keyAlgorithm='RSA-4096',certificateSha256=fingerprint,
                    keystoreFile=store.name,credentialFile='credentials.dpapi',identityLookup='%LOCALAPPDATA%/Haoke/AndroidSigning/liquidwars-release')
        jsonfile(IDENTITY/'signing-identity.json',public)
        # Establish the user's approved shared password once, encrypted for CurrentUser.
        shared=SIGNROOT/'shared-release-password'
        if not shared.exists():
            private_dir(shared);(shared/'credentials.dpapi').write_bytes(pack(credentials))
            jsonfile(shared/'credential-reference.json',dict(source_application=meta['applicationId'],source_alias=meta['alias'],
                source_certificate_sha256=meta['certificateSha256'],credentials_ref=str(shared/'credentials.dpapi')))
        # An existing shared store is never overwritten or rotated.
        jsonfile(PUBLIC,public);status('READY',certificateSha256=fingerprint,applicationId=APP_ID)
        return public
    finally:
        env.pop('LW_SIGNING_STORE_PASS',None);env.pop('LW_SIGNING_KEY_PASS',None)
        secret=None

def gui():
    from PySide6.QtCore import QObject,Signal,Qt
    from PySide6.QtGui import QFont,QFontDatabase
    from PySide6.QtWidgets import QApplication,QDialog,QVBoxLayout,QLabel,QLineEdit,QPushButton,QMessageBox
    app=QApplication(sys.argv[:1]);font=Path(os.environ['WINDIR'])/'Fonts/NotoSansSC-VF.ttf'
    if not font.is_file():raise SigningError('commercial_font_missing')
    QFontDatabase.addApplicationFont(str(font));app.setFont(QFont('Noto Sans SC',11))
    class Result(QObject):done=Signal(object)
    class Prompt(QDialog):
        def __init__(self):
            super().__init__();self.busy=False;self.result=Result();self.result.done.connect(self.finished_work)
            self.setWindowTitle('液态战争 · 创建正式签名');self.setMinimumWidth(560)
            self.setWindowFlag(Qt.WindowStaysOnTopHint,True)
            layout=QVBoxLayout(self);layout.setContentsMargins(24,24,24,24);layout.setSpacing(14)
            for s in ['为液态战争创建独立正式签名', '请输入现有的统一签名密码（用于核对安读签名）。\n不会复制安读私钥，也不会更改已有密码。',
                      '密码仅在本机处理，并以当前 Windows 用户加密保存，\n以后打包可复用。请勿把密码发送到聊天。']:
                label=QLabel(s);label.setWordWrap(True);layout.addWidget(label)
            self.password=QLineEdit();self.password.setEchoMode(QLineEdit.Password)
            self.password.setPlaceholderText('现有统一签名密码');self.password.setMinimumHeight(44);layout.addWidget(self.password)
            self.message=QLabel('新密钥保存在本机受保护签名库，源码不含密钥。');self.message.setWordWrap(True);layout.addWidget(self.message)
            self.create=QPushButton('创建正式签名');self.create.setMinimumHeight(44);self.create.clicked.connect(self.submit);layout.addWidget(self.create)
            self.cancel=QPushButton('取消');self.cancel.clicked.connect(self.reject);layout.addWidget(self.cancel)
            self.password.returnPressed.connect(self.submit)
        def submit(self):
            if self.busy or not self.password.text():return
            self.busy=True;secret=self.password.text();self.password.clear();self.password.setEnabled(False)
            self.create.setEnabled(False);self.cancel.setEnabled(False);self.message.setText('正在核对密码并创建签名，请稍候…')
            def work():
                try:answer=initialize(secret)
                except Exception as e:answer={'error':str(e) if isinstance(e,SigningError) else type(e).__name__}
                self.result.done.emit(answer)
            threading.Thread(target=work,daemon=False).start()
        def finished_work(self,answer):
            self.busy=False
            if 'error' not in answer:self.accept();return
            status('INPUT_PENDING',reason=answer['error'])
            self.password.setEnabled(True);self.create.setEnabled(True);self.cancel.setEnabled(True)
            message='密码未通过已有签名核对，请重新输入现有统一密码。' if answer['error']=='shared_password_check_failed' else '未完成签名创建：'+answer['error']+'。已有身份未被替换。'
            self.message.setText(message);self.password.setFocus()
        def closeEvent(self,event):
            if self.busy:event.ignore()
            else:super().closeEvent(event)
    status('INPUT_PENDING');prompt=Prompt();prompt.show();prompt.activateWindow();prompt.password.setFocus()
    result=prompt.exec()
    if result!=QDialog.Accepted:status('CANCELLED');return 2
    return 0

def sign(unsigned,output):
    if not unsigned.is_file() or output.exists():raise SigningError('input_missing_or_output_exists')
    sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.1'
    badging=run([sdk/'aapt.exe','dump','badging',unsigned]).decode('utf8',errors='replace')
    gradle=(ROOT/'app/build.gradle').read_text('utf8')
    version=re.search(r'versionName\s+"([^"]+)"',gradle).group(1)
    code=re.search(r'versionCode\s+(\d+)',gradle).group(1)
    if f"package: name='{APP_ID}' versionCode='{code}' versionName='{version}'" not in badging or 'application-debuggable' in badging:
        raise SigningError('wrong_input_identity_or_debuggable')
    public=json.loads(PUBLIC.read_text('utf8'));local=json.loads((IDENTITY/'signing-identity.json').read_text('utf8'))
    if public!=local or public['applicationId']!=APP_ID:raise SigningError('identity_metadata_mismatch')
    values=unpack(IDENTITY/'credentials.dpapi');env={**os.environ,'LW_SIGNING_STORE_PASS':values['store_pass'],'LW_SIGNING_KEY_PASS':values['key_pass']}
    try:
        store=IDENTITY/public['keystoreFile']
        if sha(certificate(store,ALIAS,env))!=public['certificateSha256']:raise SigningError('release_certificate_mismatch')
        run([sdk/'zipalign.exe','-c','-P','16','4',unsigned])
        run([sdk/'apksigner.bat','sign','--ks',store,'--ks-key-alias',ALIAS,'--ks-pass','env:LW_SIGNING_STORE_PASS',
             '--key-pass','env:LW_SIGNING_KEY_PASS','--v1-signing-enabled','false','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',output,unsigned],env)
        verified=run([sdk/'apksigner.bat','verify','--verbose','--print-certs',output]).decode('utf8',errors='replace')
        if public['certificateSha256'].lower() not in verified.lower():raise SigningError('signed_output_certificate_mismatch')
        print(json.dumps(dict(signed=str(output),certificateSha256=public['certificateSha256'],apk_sha256=sha(output.read_bytes()))))
    finally:
        env.pop('LW_SIGNING_STORE_PASS',None);env.pop('LW_SIGNING_KEY_PASS',None);values.clear()

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['init','init-saved','sign']);p.add_argument('--input',type=Path);p.add_argument('--output',type=Path);a=p.parse_args()
    try:
        if a.action=='init':sys.exit(gui())
        if a.action=='init-saved':initialize_saved();sys.exit(0)
        if not a.input or not a.output:p.error('sign requires --input and --output')
        sign(a.input.resolve(),a.output.resolve())
    except Exception as e:
        code=str(e) if isinstance(e,SigningError) else type(e).__name__;status('HOLD',reason=code);print('Signing HOLD:',code);sys.exit(1)
