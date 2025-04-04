# Documentação Técnica: Implementação OAuth2 no TechMel

## Sumário

1. Introdução
2. Arquitetura da Solução
3. Implementação no Backend
4. Fluxo de Autenticação Detalhado
5. Configuração do Provedor OAuth2 (Google)
6. Implementação no Frontend
7. Boas Práticas de Segurança
8. Troubleshooting
9. Referências

## 1. Introdução

O TechMel implementa autenticação OAuth2 para permitir que usuários façam login de forma segura utilizando suas contas Google. Isso proporciona uma experiência de login simplificada sem a necessidade de criar novas credenciais.

### O que é OAuth2?

OAuth2 é um protocolo de autorização que permite que aplicações terceiras acessem recursos de um serviço em nome do usuário sem a necessidade de compartilhar credenciais. No contexto do TechMel, utilizamos OAuth2 para autenticação via Google.

### Benefícios da Autenticação OAuth2

- Login simplificado para usuários
- Eliminação da necessidade de gerenciar senhas
- Maior segurança (autenticação delegada a provedores confiáveis)
- Experiência de usuário aprimorada
- Possibilidade de integração com múltiplos provedores

## 2. Arquitetura da Solução

### Diagrama de Fluxo de Autenticação

```
┌─────────────┐     1. Inicia Login     ┌───────────────┐     2. Redireciona      ┌───────────┐
│             ├────────────────────────>│               ├────────────────────────>│           │
│   Cliente   │                         │   TechMel     │                         │  Google   │
│   (SPA)     │                         │   Backend     │                         │           │
│             │<────────────────────────┤               │<────────────────────────┤           │
└─────────────┘    5. Retorna Tokens    └───────────────┘   3. Retorna com code   └───────────┘
                                              │ ▲
                                              │ │
                                          4.  │ │ Troca code por
                                              │ │ informações do usuário
                                              │ │
                                              ▼ │
                                         ┌───────────────┐
                                         │   Google      │
                                         │   API         │
                                         └───────────────┘
```

### Componentes Principais

1. **SecurityConfig**: Configura o Spring Security para suportar OAuth2
2. **OAuth2AuthenticationSuccessHandler**: Processa o login bem-sucedido
3. **OAuth2AuthenticationFailureHandler**: Trata falhas no login
4. **JwtAdapter**: Gerencia tokens JWT
5. **RefreshTokenService**: Gerencia refresh tokens
6. **Redis**: Armazena tokens temporários e blacklist de tokens

## 3. Implementação no Backend

### Configuração do OAuth2 no Spring Security

A configuração do OAuth2 é realizada na classe `SecurityConfig`:

```java
// Trecho da classe SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configurações de segurança omitidas para brevidade
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorize"))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/login/oauth2/code/*"))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );

        return http.build();
    }
}
```

### Propriedades de Configuração

No arquivo application-dev.properties:

```properties
# Configuração do OAuth2 (Google)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.redirect-uri=${REDIRECT_URI}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub

# URI após autenticação bem-sucedida
app.oauth2.redirect-uri=${FRONTEND_CALLBACK_URI}
```

### Handlers de Autenticação OAuth2

#### Handler de Sucesso

O `OAuth2AuthenticationSuccessHandler` processa o callback de sucesso:

```java
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtPort jwtPort;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final UserRepositoryPort userRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Verifica se usuário existe ou cria novo
        // ...código omitido para brevidade...

        // Gera tokens JWT e refresh
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("tokenType", "ACCESS");

        String accessToken = jwtPort.generateToken(claims, user.getEmail(), jwtExpiration);
        RefreshToken refreshToken = refreshTokenUseCase.createRefreshToken(user);

        // Armazena temporariamente no Redis com um stateId
        String stateId = UUID.randomUUID().toString();
        TokenPair tokenPair = new TokenPair(accessToken, refreshToken.getToken());
        objectRedisTemplate.opsForValue().set(
                "oauth2:state:" + stateId,
                tokenPair,
                120,
                TimeUnit.SECONDS
        );

        // Redireciona para o frontend com o stateId
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("state", stateId)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

#### Handler de Falha

O `OAuth2AuthenticationFailureHandler` trata falhas de autenticação:

```java
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        // Redireciona para o frontend com mensagem de erro
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", exception.getLocalizedMessage())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

### Controller para Troca de Tokens

O `OAuth2Controller` permite que o frontend troque o stateId pelos tokens:

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final RedisTemplate<String, Object> objectRedisTemplate;

    @GetMapping("/exchange-token")
    public ResponseEntity<Map<String, String>> exchangeToken(@RequestParam String state) {
        String redisKey = "oauth2:state:" + state;
        TokenPair tokenPair = (TokenPair) objectRedisTemplate.opsForValue().get(redisKey);

        if (tokenPair == null) {
            return ResponseEntity.notFound().build();
        }

        // Remove o token temporário após uso
        objectRedisTemplate.delete(redisKey);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", tokenPair.getAccessToken());
        response.put("refreshToken", tokenPair.getRefreshToken());

        return ResponseEntity.ok(response);
    }
}
```

## 4. Fluxo de Autenticação Detalhado

### 1. Iniciando o Fluxo de Autenticação OAuth2

1. O usuário clica no botão "Login com Google" no frontend
2. O frontend redireciona para: `/oauth2/authorize/google`
3. O Spring Security inicia o fluxo OAuth2, redirecionando para o Google

### 2. Autenticação no Google

1. O usuário se autentica no Google e concede permissões
2. O Google redireciona de volta para: `/login/oauth2/code/google` com um código de autorização

### 3. Processamento do Callback

1. O Spring Security valida o código e troca por tokens do Google
2. Spring Security obtém informações do usuário e cria um `OAuth2User`
3. O `OAuth2AuthenticationSuccessHandler` é acionado

### 4. Processamento do Login Bem-sucedido

1. Verifica se o usuário já existe no sistema:
   - Se não existir, cria um novo usuário com dados do Google
   - Se existir, verifica compatibilidade do provedor de autenticação
2. Gera tokens JWT de acesso e refresh
3. Armazena temporariamente no Redis com um stateId único
4. Redireciona para o frontend com o stateId

### 5. Troca de StateId por Tokens no Frontend

1. O frontend recebe o stateId na URL
2. O frontend faz uma requisição para: `/api/auth/exchange-token?state={stateId}`
3. O backend recupera os tokens do Redis e os retorna
4. O frontend armazena os tokens para uso futuro

### 6. Utilização dos Tokens

1. O frontend usa o accessToken para acessar recursos protegidos
2. Quando o accessToken expira, o refreshToken é usado para obter um novo
3. O fluxo de refresh é o mesmo do login tradicional

## 5. Configuração do Provedor OAuth2 (Google)

### Criando um Projeto no Google Cloud Console

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Crie um novo projeto ou selecione um existente
3. Navegue até **APIs & Serviços > Credenciais**
4. Clique em **Criar Credenciais > ID do Cliente OAuth**
5. Selecione **Aplicativo da Web**

### Configuração do Cliente OAuth2

1. **Nome do cliente**: TechMel
2. **Origens JavaScript autorizadas**: Adicione seu domínio (ex: `http://localhost:3000`)
3. **URIs de redirecionamento autorizados**: Adicione seu URI de callback
   - Formato: `{backend-url}/login/oauth2/code/google`
   - Exemplo: `http://localhost:8080/login/oauth2/code/google`
4. Clique em **Criar**
5. Anote o **Client ID** e o **Client Secret**

### Configuração no Arquivo .env

```
# Configuração do OAuth2 (Google)
GOOGLE_CLIENT_ID=seu-client-id-google
GOOGLE_CLIENT_SECRET=seu-client-secret-google
REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
FRONTEND_CALLBACK_URI=http://localhost:3000/oauth/callback
```

## 6. Implementação no Frontend

### Iniciando o Fluxo de Autenticação

```javascript
// Função para iniciar login com Google
function loginWithGoogle() {
  // Redireciona para o endpoint de autorização do backend
  window.location.href = 'http://localhost:8080/oauth2/authorize/google';
}

// Exemplo de botão de login
<button onClick={loginWithGoogle}>Login com Google</button>
```

### Processando o Callback

```javascript
// Em seu componente de callback OAuth (ex: OAuthCallback.js)
import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';

function OAuthCallback() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  
  useEffect(() => {
    // Extrair parâmetros da URL
    const params = new URLSearchParams(location.search);
    const stateId = params.get('state');
    const errorMsg = params.get('error');
    
    if (errorMsg) {
      setError(errorMsg);
      setLoading(false);
      return;
    }
    
    if (!stateId) {
      setError('Parâmetro state não encontrado');
      setLoading(false);
      return;
    }
    
    // Trocar stateId por tokens
    const exchangeToken = async () => {
      try {
        const response = await axios.get(`http://localhost:8080/api/auth/exchange-token?state=${stateId}`);
        
        // Armazenar tokens
        localStorage.setItem('accessToken', response.data.accessToken);
        localStorage.setItem('refreshToken', response.data.refreshToken);
        
        // Redirecionar para a página principal
        navigate('/dashboard');
      } catch (error) {
        setError('Falha ao trocar tokens: ' + error.message);
        setLoading(false);
      }
    };
    
    exchangeToken();
  }, [location, navigate]);
  
  if (loading) return <div>Autenticando...</div>;
  if (error) return <div>Erro: {error}</div>;
  
  return null;
}

export default OAuthCallback;
```

### Configuração do Cliente HTTP com Interceptor para Refresh

```javascript
import axios from 'axios';

// Criar instância do axios
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Interceptor para adicionar token a todas as requisições
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor para lidar com tokens expirados
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Se o erro for 401 (Unauthorized) e não for uma tentativa de refresh
    if (error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Tentar renovar o token
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post('http://localhost:8080/api/auth/refresh', {
          refreshToken,
        });
        
        // Armazenar o novo token
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        // Atualizar o cabeçalho e repetir a requisição
        originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        // Se o refresh token também estiver inválido, fazer logout
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

### Implementação do Logout

```javascript
async function logout() {
  try {
    // Chama o endpoint de logout no backend
    await api.post('/auth/logout');
    
    // Limpa tokens locais
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    
    // Redireciona para login
    window.location.href = '/login';
  } catch (error) {
    console.error('Erro ao fazer logout:', error);
    
    // Mesmo com erro, limpa tokens locais
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
  }
}
```

## 7. Boas Práticas de Segurança

### Armazenamento Seguro de Tokens

1. **No Frontend**:
   - Utilizar `localStorage` apenas em aplicações sem requisitos críticos de segurança
   - Para maior segurança, considerar cookies HttpOnly + SameSite=Strict
   - Em aplicações móveis, utilizar armazenamento seguro nativo

2. **No Backend**:
   - Tokens temporários armazenados no Redis com TTL curto (2 minutos)
   - Refresh tokens armazenados no banco de dados com relação ao usuário
   - Tokens JWT na blacklist armazenados no Redis

### Proteção Contra Ataques Comuns

1. **CSRF**: 
   - A autenticação baseada em tokens JWT mitiga naturalmente ataques CSRF
   - Uso de state parameter no fluxo OAuth2 previne ataques de replay

2. **XSS**:
   - No frontend, evitar `innerHTML` e usar métodos seguros como `textContent`
   - Implementar CSP (Content Security Policy)
   - Validar todas as entradas de usuário

3. **Proteção de Redirecionamento**:
   - Validar todos os redirecionamentos contra lista de URLs permitidos
   - Nunca aceitar URLs de redirecionamento dinâmicos de fontes não confiáveis

### Gerenciamento de Sessões

1. **Duração dos Tokens**:
   - Access tokens: curta duração (30 minutos)
   - Refresh tokens: longa duração (configurável)

2. **Revogação de Sessões**:
   - Implementação de logout que revoga tokens ativos
   - Capacidade de revogar todas as sessões de um usuário
   - Blacklist de tokens JWT revogados

### Atualizações Regulares

1. Manter dependências atualizadas (Spring Security, bibliotecas JWT)
2. Verificar regularmente por vulnerabilidades em dependências 
3. Revisar e atualizar configurações de segurança periodicamente

## 8. Troubleshooting

### Problemas Comuns e Soluções

1. **Erro "redirect_uri_mismatch"**
   - **Sintoma**: O Google retorna erro informando incompatibilidade de URI de redirecionamento
   - **Solução**: Verificar se a URI configurada no Google Cloud Console corresponde exatamente à URI em application.properties

2. **Tokens Expirados**
   - **Sintoma**: Requisições retornando 401 mesmo após refresh token
   - **Solução**: Verificar implementação do interceptor, logs do backend para problemas com refresh token

3. **Problemas com CORS**
   - **Sintoma**: Erros de CORS no console do navegador
   - **Solução**: Verificar configurações de CORS no backend, garantir que os domínios corretos estão permitidos

4. **Callback com Erro após Autenticação**
   - **Sintoma**: Frontend recebe parâmetro de erro na URL de callback
   - **Solução**: Verificar logs do backend, configurações do OAuth2, permissões concedidas no Google

### Logs para Depuração

Ativar logs detalhados no application-dev.properties:

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.web.client.RestTemplate=DEBUG
```

## 9. Referências

- [OAuth 2.0 Specification](https://oauth.net/2/)
- [Spring Security OAuth2 Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Google Identity Platform](https://developers.google.com/identity)
- [JWT.io](https://jwt.io/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

---

Esta documentação abrange todos os aspectos da implementação OAuth2 no sistema TechMel. Para dúvidas adicionais ou suporte técnico, entre em contato com a equipe de desenvolvimento.