<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-visual">
        <div class="visual-content">
          <Aperture :size="48" class="visual-logo" />
          <h1 class="visual-title">PVMS STUDIO</h1>
          <p class="visual-desc">Professional Visual Management System</p>
        </div>
        <div class="visual-overlay"></div>
      </div>
      
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Aperture, User, Lock, Loader2, 
  ShieldCheck, UserCircle, FileText 
} from 'lucide-vue-next'
import { authApi } from '@/api/auth'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const captchaImg = ref('')

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

const refreshCaptcha = async () => {
  try {
    const res = await authApi.getCaptcha()
    captchaImg.value = res.img
  } catch (err) {
    console.error('Failed to load captcha', err)
  }
}

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await authApi.login(loginForm)
    localStorage.setItem('token', res)
    router.push('/')
  } catch (err: any) {
    alert(err.message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    const res = await authApi.register(registerForm)
    alert(res || '申请已提交')
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
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg);
  background-image: radial-gradient(var(--border) 1px, transparent 1px);
  background-size: 32px 32px;
  padding: 20px;
}

.auth-card {
  width: 1000px;
  max-width: 100%;
  height: 640px;
  background: var(--surface);
  border-radius: 40px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--border);
}

.auth-visual {
  flex: 1.2;
  background: var(--primary);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: white;
}

.visual-overlay {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(45deg, rgba(37, 99, 235, 0.2) 0%, transparent 70%),
    url('https://images.unsplash.com/photo-1493397212122-2b85def82820?q=80&w=2070&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  opacity: 0.8;
}

.visual-content {
  position: relative;
  z-index: 10;
  text-align: center;
}

.visual-logo {
  color: var(--accent);
  margin-bottom: 24px;
  filter: drop-shadow(0 0 15px rgba(37, 99, 235, 0.5));
}

.visual-title {
  font-size: 2.5rem;
  font-weight: 900;
  letter-spacing: -1px;
  margin-bottom: 8px;
}

.visual-desc {
  font-size: 1rem;
  opacity: 0.7;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.auth-form-container {
  flex: 1;
  padding: 60px;
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
}

.captcha-input {
  flex: 1;
}

.captcha-img-box {
  width: 120px;
  height: 48px;
  background: var(--surface-hover);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
    flex-direction: column;
    height: auto;
    width: 100%;
    max-width: 500px;
  }
  .auth-visual {
    padding: 40px;
  }
  .visual-title {
    font-size: 1.75rem;
  }
}
</style>
