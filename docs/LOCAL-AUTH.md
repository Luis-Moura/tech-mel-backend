# Documentação Técnica: Autenticação Local no TechMel

## Sumário

1. Introdução
2. Arquitetura da Solução
3. Implementação no Backend
4. Fluxo de Autenticação Detalhado
5. Implementação no Frontend
6. Boas Práticas de Segurança
7. Troubleshooting
8. Referências

## 1. Introdução

O TechMel implementa autenticação local baseada em email e senha, permitindo que usuários se autentiquem diretamente no sistema. Esta documentação descreve detalhadamente os aspectos técnicos dessa implementação.

### Visão Geral da Autenticação Local

A autenticação local refere-se ao processo de verificar a identidade de um usuário através de credenciais (email/senha) armazenadas e gerenciadas pelo próprio sistema, sem depender de serviços externos de identidade.

### Benefícios da Autenticação Local

- Controle total sobre o processo de autenticação
- Independência de provedores de autenticação externos
- Possibilidade de personalização completa do fluxo de login
- Compatibilidade universal com todos os dispositivos e navegadores
- Menor complexidade para usuários acostumados com fluxos tradicionais

## 2. Arquitetura da Solução

### Diagrama de Fluxo de Autenticação

```
┌─────────────┐     1. Envia Credenciais     ┌───────────────┐
│             ├─────────────────────────────>│               │
│   Cliente   │                              │   TechMel     │
│   (SPA)     │                              │   Backend     │
│             │<─────────────────────────────┤               │
└─────────────┘     2. Retorna Tokens        └───────────────┘
       │                                            │
       │                                            │
       │ 3. Solicita recursos com                   │ 4. Valida tokens
       │    Authorization Bearer                    │    e autoriza acesso
       ▼                                            ▼
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                       API Recursos                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Componentes Principais

1. **AuthenticationController**: Processa requisições de login/registro
2. **UserService**: Gerencia operações relacionadas aos usuários
3. **PasswordEncoder**: Responsável pelo hashing seguro de senhas
4. **JwtService**: Gerencia geração e validação de tokens JWT
5. **RefreshTokenService**: Gerencia tokens de atualização
6. **SecurityConfig**: Configura o Spring Security
7. **JwtAuthenticationFilter**: Filtra requisições para validação de tokens

## 3. Implementação no Backend

### Entidade de Usuário

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private String firstName;
    private String lastName;

    @Column(nullable = false)
    private Boolean enabled = true;

    // Métodos auxiliares omitidos para brevidade
}
```

### Configuração do Spring Security

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Configuração do Authentication Provider

```java
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Implementação do JWT Authentication Filter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // Verifica se o token está na blacklist
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e) {
            // Log dos erros, mas não interrompe o fluxo do filtro
            // Deixa o Spring Security lidar com a falha de autenticação
        }

        filterChain.doFilter(request, response);
    }
}
```

### Serviço de JWT

```java
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.security.jwt.secret-key}")
    private String secretKey;

    @Value("${app.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        User user = (User) userDetails;

        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("tokenType", "ACCESS");

        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Serviço de Refresh Token

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.security.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public RefreshToken createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(
                    token.getToken(),
                    "Refresh token expirado. Por favor, faça login novamente."
            );
        }

        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
```

### Controller de Autenticação

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) userService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return ResponseEntity.ok(AuthenticationResponse.builder()
                            .accessToken(token)
                            .refreshToken(requestRefreshToken)
                            .build());
                })
                .orElseThrow(() -> new TokenRefreshException(
                        requestRefreshToken,
                        "Refresh token não encontrado!"
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest request
    ) {
        String jwt = authHeader.substring(7);
        String refreshToken = request.getRefreshToken();

        // Adiciona o token JWT atual à blacklist
        tokenBlacklistService.blacklist(jwt);

        // Tenta invalidar o refresh token
        refreshTokenService.findByToken(refreshToken)
                .ifPresent(token -> refreshTokenService.deleteByUserId(token.getUser().getId()));

        return ResponseEntity.ok().build();
    }
}
```

### Models para Requisições e Respostas

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String password;
}

// filepath: src/main/java/com/techmel/dto/RegisterRequest.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String firstName;

    @NotBlank(message = "Sobrenome é obrigatório")
    private String lastName;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String password;
}

// filepath: src/main/java/com/techmel/dto/AuthenticationResponse.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
}

// filepath: src/main/java/com/techmel/dto/TokenRefreshRequest.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}

// filepath: src/main/java/com/techmel/dto/LogoutRequest.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
```

### Serviço de Blacklist para Tokens Revogados

```java
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    // Adiciona um token à blacklist até sua data de expiração
    public void blacklist(String token) {
        try {
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                String key = "blacklist:" + token;
                redisTemplate.opsForValue().set(key, "blacklisted");
                redisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Log do erro, mas não impede a operação
        }
    }

    // Verifica se um token está na blacklist
    public boolean isBlacklisted(String token) {
        try {
            String key = "blacklist:" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // Em caso de erro, assume que o token não está na blacklist
            return false;
        }
    }
}
```

## 4. Fluxo de Autenticação Detalhado

### 1. Registro de Usuário

1. O cliente envia uma requisição POST para `/api/auth/register` com os dados do usuário
2. O backend valida os dados de entrada
3. Verifica se o email já está registrado
4. Criptografa a senha usando BCrypt
5. Persiste o usuário no banco de dados
6. O cliente recebeum email de confirmação para verificar sua conta
7. O email de verificação redireciona o usuário com o token para `/api/auth/verify`
8. Por fim o cliente pode ser cadastrado ou receber uma resposta de erro, no caso o frontend deve lidar com cada possibilidade

### 2. Autenticação (Login)

1. O cliente envia uma requisição POST para `/api/auth/authenticate` com email e senha
2. O `AuthenticationManager` valida as credenciais utilizando o `PasswordEncoder`
3. Se as credenciais são válidas:
   - Um token JWT de acesso é gerado com as informações do usuário
   - Um refresh token é criado e armazenado no banco de dados
   - Ambos os tokens são retornados ao cliente
4. Se as credenciais são inválidas, uma exceção `BadCredentialsException` é lançada

### 3. Acesso a Recursos Protegidos

1. O cliente inclui o token JWT no cabeçalho Authorization de cada requisição
2. O `JwtAuthenticationFilter` intercepta cada requisição:
   - Extrai o token do cabeçalho
   - Verifica se o token está na blacklist
   - Valida a assinatura e expiração do token
   - Extrai o username (email) e informações do usuário
   - Carrega os detalhes do usuário do banco de dados
   - Configura o `SecurityContext` com a autenticação
3. Os controllers verificam as autorizações através de anotações como `@PreAuthorize`
4. Os recursos são retornados se o usuário tem as permissões necessárias

### 4. Renovação do Token de Acesso

1. Quando o token de acesso expira, o cliente envia uma requisição POST para `/api/auth/refresh-token`
2. O backend verifica se o refresh token:
   - Existe no banco de dados
   - Não expirou
   - Está associado a um usuário válido
3. Se o refresh token é válido:
   - Um novo token JWT de acesso é gerado
   - O mesmo refresh token é mantido
   - Ambos são retornados ao cliente
4. Se o refresh token é inválido, um erro 401 é retornado

### 5. Logout

1. O cliente envia uma requisição POST para `/api/auth/logout` com o token de acesso e refresh
2. O backend:
   - Adiciona o token JWT atual à blacklist no Redis
   - Remove o refresh token do banco de dados
   - Retorna um status 200 OK
3. O cliente remove os tokens armazenados localmente

## 5. Implementação no Frontend

### Configuração do Cliente HTTP

```javascript
import axios from "axios";

// Criação da instância base do axios
const api = axios.create({
	baseURL: "http://localhost:8080/api",
	headers: {
		"Content-Type": "application/json",
	},
});

// Interceptor para adicionar o token de acesso a todas as requisições
api.interceptors.request.use(
	(config) => {
		const token = localStorage.getItem("accessToken");
		if (token) {
			config.headers["Authorization"] = `Bearer ${token}`;
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
		if (error.response?.status === 401 && !originalRequest._retry) {
			originalRequest._retry = true;

			try {
				// Tenta renovar o token usando o refresh token
				const refreshToken = localStorage.getItem("refreshToken");
				if (!refreshToken) {
					// Se não houver refresh token, redireciona para login
					logoutAndRedirect();
					return Promise.reject(error);
				}

				const response = await axios.post(
					"http://localhost:8080/api/auth/refresh-token",
					{
						refreshToken,
					}
				);

				// Armazena o novo token de acesso
				const { accessToken } = response.data;
				localStorage.setItem("accessToken", accessToken);

				// Atualiza o cabeçalho e repete a requisição original
				originalRequest.headers["Authorization"] = `Bearer ${accessToken}`;
				return axios(originalRequest);
			} catch (refreshError) {
				// Se o refresh token também estiver inválido, faz logout
				logoutAndRedirect();
				return Promise.reject(refreshError);
			}
		}

		return Promise.reject(error);
	}
);

// Função auxiliar para logout e redirecionamento
function logoutAndRedirect() {
	localStorage.removeItem("accessToken");
	localStorage.removeItem("refreshToken");
	window.location.href = "/login";
}

export default api;
```

### Serviço de Autenticação

```javascript
import api from "./api";

const AuthService = {
	// Registro de novo usuário
	register: async (userData) => {
		const response = await api.post("/auth/register", userData);
		if (response.data.accessToken) {
			localStorage.setItem("accessToken", response.data.accessToken);
			localStorage.setItem("refreshToken", response.data.refreshToken);
		}
		return response.data;
	},

	// Login de usuário
	login: async (email, password) => {
		const response = await api.post("/auth/authenticate", { email, password });
		if (response.data.accessToken) {
			localStorage.setItem("accessToken", response.data.accessToken);
			localStorage.setItem("refreshToken", response.data.refreshToken);
		}
		return response.data;
	},

	// Logout de usuário
	logout: async () => {
		try {
			const refreshToken = localStorage.getItem("refreshToken");
			await api.post("/auth/logout", { refreshToken });
		} catch (error) {
			console.error("Erro ao fazer logout no servidor:", error);
		} finally {
			localStorage.removeItem("accessToken");
			localStorage.removeItem("refreshToken");
		}
	},

	// Verificar se o usuário está autenticado
	isAuthenticated: () => {
		return !!localStorage.getItem("accessToken");
	},

	// Atualizar token usando refresh token
	refreshToken: async () => {
		const refreshToken = localStorage.getItem("refreshToken");
		if (!refreshToken) {
			throw new Error("Refresh token não encontrado");
		}

		const response = await api.post("/auth/refresh-token", { refreshToken });
		if (response.data.accessToken) {
			localStorage.setItem("accessToken", response.data.accessToken);
			return response.data;
		}
	},
};

export default AuthService;
```

### Componente de Formulário de Login

```jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthService from "../services/authService";

function Login() {
	const [credentials, setCredentials] = useState({
		email: "",
		password: "",
	});
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const handleChange = (e) => {
		const { name, value } = e.target;
		setCredentials({
			...credentials,
			[name]: value,
		});
	};

	const handleSubmit = async (e) => {
		e.preventDefault();
		setLoading(true);
		setError("");

		try {
			await AuthService.login(credentials.email, credentials.password);
			navigate("/dashboard");
		} catch (err) {
			setError(
				err.response?.data?.message ||
					"Falha na autenticação. Verifique suas credenciais."
			);
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="login-container">
			<h2>Login</h2>

			{error && <div className="alert alert-danger">{error}</div>}

			<form onSubmit={handleSubmit}>
				<div className="form-group">
					<label htmlFor="email">Email</label>
					<input
						type="email"
						className="form-control"
						id="email"
						name="email"
						value={credentials.email}
						onChange={handleChange}
						required
					/>
				</div>

				<div className="form-group">
					<label htmlFor="password">Senha</label>
					<input
						type="password"
						className="form-control"
						id="password"
						name="password"
						value={credentials.password}
						onChange={handleChange}
						required
					/>
				</div>

				<button type="submit" className="btn btn-primary" disabled={loading}>
					{loading ? "Processando..." : "Entrar"}
				</button>
			</form>

			<div className="mt-3">
				<p>
					Não tem uma conta? <a href="/register">Registre-se</a>
				</p>
			</div>
		</div>
	);
}

export default Login;
```

### Componente de Formulário de Registro

```jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthService from "../services/authService";

function Register() {
	const [userData, setUserData] = useState({
		firstName: "",
		lastName: "",
		email: "",
		password: "",
		confirmPassword: "",
	});
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const handleChange = (e) => {
		const { name, value } = e.target;
		setUserData({
			...userData,
			[name]: value,
		});
	};

	const validateForm = () => {
		if (userData.password !== userData.confirmPassword) {
			setError("As senhas não coincidem");
			return false;
		}
		if (userData.password.length < 8) {
			setError("A senha deve ter pelo menos 8 caracteres");
			return false;
		}
		return true;
	};

	const handleSubmit = async (e) => {
		e.preventDefault();
		if (!validateForm()) return;

		setLoading(true);
		setError("");

		try {
			// Remove confirmPassword antes de enviar para a API
			const { confirmPassword, ...registrationData } = userData;
			await AuthService.register(registrationData);
			navigate("/dashboard");
		} catch (err) {
			if (err.response?.data?.message) {
				setError(err.response.data.message);
			} else if (err.response?.status === 400) {
				setError("Dados inválidos. Por favor, verifique seus dados.");
			} else {
				setError("Erro ao registrar. Tente novamente mais tarde.");
			}
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="register-container">
			<h2>Registrar</h2>

			{error && <div className="alert alert-danger">{error}</div>}

			<form onSubmit={handleSubmit}>
				<div className="form-group">
					<label htmlFor="firstName">Nome</label>
					<input
						type="text"
						className="form-control"
						id="firstName"
						name="firstName"
						value={userData.firstName}
						onChange={handleChange}
						required
					/>
				</div>

				<div className="form-group">
					<label htmlFor="lastName">Sobrenome</label>
					<input
						type="text"
						className="form-control"
						id="lastName"
						name="lastName"
						value={userData.lastName}
						onChange={handleChange}
						required
					/>
				</div>

				<div className="form-group">
					<label htmlFor="email">Email</label>
					<input
						type="email"
						className="form-control"
						id="email"
						name="email"
						value={userData.email}
						onChange={handleChange}
						required
					/>
				</div>

				<div className="form-group">
					<label htmlFor="password">Senha</label>
					<input
						type="password"
						className="form-control"
						id="password"
						name="password"
						value={userData.password}
						onChange={handleChange}
						required
						minLength="8"
					/>
				</div>

				<div className="form-group">
					<label htmlFor="confirmPassword">Confirmar Senha</label>
					<input
						type="password"
						className="form-control"
						id="confirmPassword"
						name="confirmPassword"
						value={userData.confirmPassword}
						onChange={handleChange}
						required
					/>
				</div>

				<button type="submit" className="btn btn-primary" disabled={loading}>
					{loading ? "Processando..." : "Registrar"}
				</button>
			</form>

			<div className="mt-3">
				<p>
					Já tem uma conta? <a href="/login">Entre aqui</a>
				</p>
			</div>
		</div>
	);
}

export default Register;
```

### Componente Protegido (Exemplo)

```jsx
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import AuthService from "../services/authService";

function Dashboard() {
	const [userData, setUserData] = useState(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");
	const navigate = useNavigate();

	useEffect(() => {
		const fetchUserData = async () => {
			try {
				const response = await api.get("/users/me");
				setUserData(response.data);
			} catch (err) {
				setError("Falha ao carregar dados do usuário");
				console.error(err);
			} finally {
				setLoading(false);
			}
		};

		fetchUserData();
	}, []);

	const handleLogout = async () => {
		await AuthService.logout();
		navigate("/login");
	};

	if (loading) return <div>Carregando...</div>;
	if (error) return <div className="alert alert-danger">{error}</div>;

	return (
		<div className="dashboard-container">
			<h1>Dashboard</h1>
			{userData && (
				<div className="user-info">
					<h2>Bem-vindo, {userData.firstName}!</h2>
					<p>Email: {userData.email}</p>
					<p>Perfil: {userData.role}</p>
				</div>
			)}

			<button className="btn btn-danger" onClick={handleLogout}>
				Sair
			</button>
		</div>
	);
}

export default Dashboard;
```

### Proteção de Rotas

```jsx
import React from "react";
import { Navigate, Outlet } from "react-router-dom";
import AuthService from "../services/authService";

function PrivateRoute() {
	const isAuthenticated = AuthService.isAuthenticated();

	return isAuthenticated ? <Outlet /> : <Navigate to="/login" />;
}

export default PrivateRoute;
```

### Configuração das Rotas

```jsx
import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./components/Login";
import Register from "./components/Register";
import Dashboard from "./components/Dashboard";
import PrivateRoute from "./components/PrivateRoute";

function App() {
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/login" element={<Login />} />
				<Route path="/register" element={<Register />} />

				{/* Rotas protegidas */}
				<Route element={<PrivateRoute />}>
					<Route path="/dashboard" element={<Dashboard />} />
					{/* Adicione outras rotas protegidas aqui */}
				</Route>

				{/* Redirecionamento da raiz */}
				<Route path="/" element={<Navigate to="/dashboard" />} />

				{/* Rota de fallback */}
				<Route path="*" element={<Navigate to="/login" />} />
			</Routes>
		</BrowserRouter>
	);
}

export default App;
```

## 6. Boas Práticas de Segurança

### Hashing de Senhas

O sistema utiliza BCrypt para armazenamento seguro de senhas:

- Algoritmo projetado especificamente para hashing de senhas
- Implementa salt automaticamente para prevenir ataques rainbow table
- É naturalmente lento, o que dificulta ataques de força bruta
- Configurado através do `PasswordEncoder` no Spring Security

### Proteção Contra Ataques Brute Force

1. **Rate Limiting**:

   - Implementar limitação de tentativas de login por IP
   - Aumentar progressivamente o tempo de espera entre tentativas
   - *OBS*: ISSO JÁ É IMPLEMENTADO EM CADA ROTA DO BACKEND

2. **CAPTCHA**:
   - Implementar CAPTCHA após múltiplas tentativas falhas de login
   - Usar serviços como reCAPTCHA em formulários de login

### Proteção Contra SQL Injection

1. **Uso de JPA/Hibernate**:

   - Utiliza consultas parametrizadas automaticamente
   - Evita concatenação de strings em consultas SQL

2. **Input Validation**:
   - Validação dos dados de entrada usando anotações Bean Validation
   - Sanitização de inputs antes do processamento

### Proteção Contra XSS

1. **No Backend**:

   - Escape automático de caracteres especiais pelo Spring MVC
   - Content-Security-Policy nos cabeçalhos HTTP

2. **No Frontend**:
   - Uso de frameworks como React que escapam automaticamente o conteúdo
   - Evitar o uso de `innerHTML` ou `dangerouslySetInnerHTML` sem sanitização

### Políticas de Senhas Fortes

1. **Validação no Registro**:

   - Exigir comprimento mínimo (8 caracteres)
   - Exigir complexidade (letras, números, caracteres especiais)
   - Verificar contra listas de senhas comuns

2. **Implementação de Validador de Senha**:

```java
@Component
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Cria um validador de senha com regras
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                // Comprimento entre 8 e 30 caracteres
                new LengthRule(8, 30),
                // Pelo menos um caractere maiúsculo
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                // Pelo menos um caractere minúsculo
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                // Pelo menos um dígito
                new CharacterRule(EnglishCharacterData.Digit, 1),
                // Pelo menos um caractere especial
                new CharacterRule(EnglishCharacterData.Special, 1),
                // Sem espaços em branco
                new WhitespaceRule()
        ));

        RuleResult result = validator.validate(new PasswordData(password));
        return result.isValid();
    }
}
```

### Configurações de Segurança do JWT

1. **Tempo de Vida Limitado**:

   - Access tokens com duração curta (30 minutos)
   - Refresh tokens com duração mais longa (7-30 dias)

2. **Assinatura Segura**:

   - Uso de chaves fortes (mínimo 256 bits)
   - Rotação periódica das chaves

3. **Claims Específicas**:
   - Incluir apenas o necessário nas claims
   - Verificar o emissor, público e expiração

### Proteção de Endpoints

1. **CORS Configurado Corretamente**:

   - Restringir origens permitidas aos domínios da aplicação
   - Especificar métodos e cabeçalhos permitidos

2. **Rate Limiting em Endpoints Críticos**:

   - Implementar limitação de requisições por IP/usuário
   - Aplicar ratelimit em endpoints sensíveis (login, registro, reset de senha)

3. **Auditoria e Logging**:
   - Registrar eventos de segurança (login, logout, alteração de permissões)
   - Implementar sistema de auditoria para operações críticas

## 7. Troubleshooting

### Problemas Comuns no Backend

#### 1. Token JWT Inválido ou Expirado

**Sintoma**: Requisições retornando 401 Unauthorized

**Possíveis Causas**:

- Token expirado
- Assinatura inválida
- Token na blacklist

**Soluções**:

- Verificar a configuração de tempo de expiração dos tokens
- Confirmar que a chave de assinatura é a mesma em toda a aplicação
- Verificar logs para detalhes sobre a falha na validação

#### 2. Falha no Refresh Token

**Sintoma**: Não é possível obter novo token de acesso

**Possíveis Causas**:

- Refresh token expirado
- Refresh token inválido ou não encontrado
- Problemas no banco de dados

**Soluções**:

- Verificar se o refresh token está armazenado corretamente
- Confirmar a configuração de expiração do refresh token
- Validar se o refresh token existe no banco de dados

#### 3. Problemas de Autorização

**Sintoma**: Erro 403 Forbidden ao acessar recursos

**Possíveis Causas**:

- Permissões insuficientes para o usuário
- Configuração incorreta de autorização
- Problemas na extração de roles do token

**Soluções**:

- Verificar as roles do usuário no banco de dados
- Conferir as anotações `@PreAuthorize` nos controllers e `hasAnyAuthority` ou `hasAuthority` no SpringSecurity
- Analisar o token JWT para confirmar a presença das claims corretas

### Problemas Comuns no Frontend

#### 1. Falhas de Autenticação

**Sintoma**: Usuário não consegue fazer login

**Possíveis Causas**:

- Credenciais incorretas
- Problemas de conexão com o backend
- CORS não configurado corretamente

**Soluções**:

- Verificar se as credenciais estão corretas
- Confirmar que o backend está acessível
- Validar configuração de CORS no backend

#### 2. Logout Inesperado

**Sintoma**: Usuário é deslogado sem realizar a ação de logout

**Possíveis Causas**:

- Token de acesso expirado
- Refresh token expirado
- Falha no processo de refresh token

**Soluções**:

- Revisar a lógica do interceptor de refresh token
- Verificar se os tokens estão sendo armazenados corretamente
- Aumentar o tempo de vida dos tokens se necessário

#### 3. Token Não Persistente Após Refresh da Página

**Sintoma**: Usuário perde autenticação ao atualizar a página

**Possíveis Causas**:

- Armazenamento incorreto de tokens
- Problemas com localStorage ou cookies
- Falha na inicialização do estado de autenticação

**Soluções**:

- Verificar se os tokens estão sendo salvos corretamente no localStorage
- Implementar verificação de autenticação no carregamento da aplicação
- Considerar o uso de cookies HttpOnly para maior segurança

### Logs para Depuração

Ativar logs detalhados no `application-dev.properties`:

```properties
# Configuração de logs para depuração
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.com.techmel=TRACE
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## 8. Referências

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [BCrypt Documentation](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [React Authentication Best Practices](https://reactjs.org/docs/security.html)

---

Esta documentação abrange todos os aspectos da implementação de autenticação local no sistema TechMel. Para dúvidas adicionais ou suporte técnico, entre em contato com a equipe de desenvolvimento.
