<template>
  <div class="auth-container">
    <transition name="notice-pop">
      <div v-if="notice.visible" class="notice-overlay">
        <div class="notice-card" role="alert">
          <div class="notice-title">登录失败</div>
          <div class="notice-message">{{ notice.message }}</div>
          <button type="button" class="notice-action" @click="hideNotice">知道了</button>
        </div>
      </div>
    </transition>

    <div class="auth-card">
      <div class="auth-form-container">
        <!-- Login Form -->
        <transition name="fade-slide" mode="out-in">
          <div v-if="mode === 'login'" class="form-content" key="login">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">请输入您的凭据以访问工作空间</p>
            
            <form @submit.prevent="handleLogin" class="auth-form">
              <div class="input-group">
                <label>用户名</label>
                <div class="input-wrapper">
                  <User :size="18" class="input-icon" />
                  <input v-model="loginForm.username" type="text" placeholder="请输入用户名" required>
                </div>
              </div>
              
              <div class="input-group">
                <label>密码</label>
                <div class="input-wrapper">
                  <Lock :size="18" class="input-icon" />
                  <input v-model="loginForm.password" type="password" placeholder="请输入密码" required>
                </div>
              </div>

              <div class="input-group">
                <label>验证码</label>
                <div class="captcha-row">
                  <div class="input-wrapper captcha-input">
                    <ShieldCheck :size="18" class="input-icon" />
                    <input v-model="loginForm.captcha" type="text" placeholder="验证码" required>
                  </div>
                  <div class="captcha-img-box" @click="refreshCaptcha">
                    <img v-if="captchaImg" :src="captchaImg" alt="captcha">
                    <div v-else class="captcha-loading">加载中...</div>
                  </div>
                </div>
              </div>
              
              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="!loading">登录</span>
                <Loader2 v-else class="animate-spin" />
              </button>
            </form>
            
            <p class="switch-mode">
              没有账号？ <a @click="mode = 'register'">提交访问申请</a>
            </p>
          </div>

          <!-- Register Form -->
          <div v-else class="form-content" key="register">
            <h2 class="form-title">访问申请</h2>
            <p class="form-subtitle">请填写以下信息，管理员审批后即可访问</p>
            
            <form @submit.prevent="handleRegister" class="auth-form">
              <div class="input-row">
                <div class="input-group">
                  <label>用户名</label>
                  <div class="input-wrapper">
                    <User :size="18" class="input-icon" />
                    <input v-model="registerForm.username" type="text" placeholder="设置用户名" required>
                  </div>
                </div>
                <div class="input-group">
                  <label>申请人姓名</label>
                  <div class="input-wrapper">
                    <UserCircle :size="18" class="input-icon" />
                    <input v-model="registerForm.applicant" type="text" placeholder="真实姓名" required>
                  </div>
                </div>
              </div>
              
              <div class="input-group">
                <label>密码</label>
                <div class="input-wrapper">
                  <Lock :size="18" class="input-icon" />
                  <input v-model="registerForm.password" type="password" placeholder="设置登录密码" required>
                </div>
              </div>

              <div class="input-group">
                <label>申请理由</label>
                <div class="input-wrapper">
                  <FileText :size="18" class="input-icon" />
                  <input v-model="registerForm.reason" type="text" placeholder="例如：摄影后期合作、样片查看等" required>
                </div>
              </div>

              <div class="input-group">
                <label>验证码</label>
                <div class="captcha-row">
                  <div class="input-wrapper captcha-input">
                    <ShieldCheck :size="18" class="input-icon" />
                    <input v-model="registerForm.captcha" type="text" placeholder="验证码" required>
                  </div>
                  <div class="captcha-img-box" @click="refreshCaptcha">
                    <img v-if="captchaImg" :src="captchaImg" alt="captcha">
                  </div>
                </div>
              </div>
              
              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="!loading">提交申请</span>
                <Loader2 v-else class="animate-spin" />
              </button>
            </form>
            
            <p class="switch-mode">
              已有账号？ <a @click="mode = 'login'">立即登录</a>
            </p>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, Lock, Loader2, 
  ShieldCheck, UserCircle, FileText 
} from 'lucide-vue-next'
import { authApi } from '@/api/auth'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const captchaImg = ref('')
// 保存后端返回的验证码 key，登录时与用户输入的验证码一起提交。
const captchaKey = ref('')
// 页面内居中提示框状态，用来替代浏览器原生 alert。
const notice = reactive({
  visible: false,
  message: ''
})
let noticeTimer: ReturnType<typeof setTimeout> | undefined

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  applicant: '',
  reason: '',
  captcha: ''
})

const resetRegisterForm = () => {
  Object.assign(registerForm, {
    username: '',
    password: '',
    applicant: '',
    reason: '',
    captcha: ''
  })
}

const refreshCaptcha = async () => {
  try {
    const res = await authApi.getCaptcha()
    captchaImg.value = res.img
    // 每次刷新验证码都同步替换 key，并清空旧输入，避免旧验证码误提交。
    captchaKey.value = res.key
    loginForm.captcha = ''
    registerForm.captcha = ''
  } catch (err) {
    console.error('Failed to load captcha', err)
  }
}

// 展示居中圆角提示框，并在短暂延迟后自动关闭。
const showNotice = (message: string) => {
  notice.message = message
  notice.visible = true

  if (noticeTimer) {
    clearTimeout(noticeTimer)
  }

  noticeTimer = setTimeout(() => {
    notice.visible = false
  }, 2800)
}

// 手动关闭提示框，同时清理自动关闭定时器。
const hideNotice = () => {
  notice.visible = false
  if (noticeTimer) {
    clearTimeout(noticeTimer)
    noticeTimer = undefined
  }
}

const handleLogin = async () => {
  loading.value = true
  try {
    // 登录认证需要同时提交验证码文本和验证码 key，后端据此完成一次性校验。
    const res = await authApi.login({
      ...loginForm,
      captchaKey: captchaKey.value
    })
    localStorage.setItem('token', res)
    router.push('/media')
  } catch (err: any) {
    showNotice(err.message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    const res = await authApi.register({
      ...registerForm,
      captchaKey: captchaKey.value
    })
    alert(res || '申请已提交')
    resetRegisterForm()
    mode.value = 'login'
  } catch (err: any) {
    alert(err.message || '申请失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

watch(mode, () => {
  refreshCaptcha()
})
</script>

<style scoped>
.auth-container {
  flex: 1;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg);
  background-image: radial-gradient(var(--border) 1px, transparent 1px);
  background-size: 32px 32px;
  padding: 20px;
}

.notice-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(2px);
}

.notice-card {
  width: min(360px, 100%);
  padding: 24px;
  border-radius: 22px;
  border: 1px solid rgba(248, 113, 113, 0.28);
  background: var(--surface);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
  text-align: center;
}

.notice-title {
  margin-bottom: 8px;
  color: #dc2626;
  font-size: 1.05rem;
  font-weight: 800;
}

.notice-message {
  color: var(--text);
  font-size: 0.95rem;
  line-height: 1.6;
  word-break: break-word;
}

.notice-action {
  margin-top: 18px;
  min-width: 108px;
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.notice-pop-enter-active,
.notice-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.notice-pop-enter-from,
.notice-pop-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.auth-card {
  width: 480px;
  max-width: 100%;
  min-height: 560px;
  background: var(--surface);
  border-radius: 28px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--border);
}

.auth-form-container {
  flex: 1;
  padding: 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.form-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.form-subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
  margin-bottom: 32px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.input-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text);
  padding-left: 4px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  color: var(--text-muted);
}

.input-wrapper input {
  width: 100%;
  padding: 14px 14px 14px 48px;
  background: var(--surface-hover);
  border: 1px solid var(--border);
  border-radius: 16px;
  outline: none;
  font-size: 0.95rem;
  color: var(--text);
  transition: all 0.2s;
}

.input-wrapper input:focus {
  border-color: var(--accent);
  background: var(--surface);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img-box {
  width: 140px;
  height: 48px;
  background: var(--surface-hover);
  border-radius: 12px;
  overflow: visible;
  cursor: pointer;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 8px;
}

.captcha-img-box img {
  width: 100%;
  height: auto;
  max-height: 40px;
  object-fit: contain;
}

.submit-btn {
  margin-top: 12px;
  padding: 16px;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 18px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(37, 99, 235, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.switch-mode {
  margin-top: 24px;
  text-align: center;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.switch-mode a {
  color: var(--accent);
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
}

.switch-mode a:hover {
  text-decoration: underline;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@media (max-width: 900px) {
  .auth-card {
    height: auto;
    width: 100%;
    max-width: 480px;
  }

  .auth-form-container {
    padding: 40px 28px;
  }

  .input-row {
    grid-template-columns: 1fr;
  }
}
</style>
