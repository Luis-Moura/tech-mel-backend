# Documentação do Sistema TechMel

## Parte 1: Documentação de Autenticação, Autorização e Segurança

### 1. Visão Geral do Sistema de Autenticação

O sistema de autenticação do TechMel foi implementado seguindo as melhores práticas de segurança, utilizando uma abordagem baseada em tokens JWT com mecanismo de refresh token. Este design permite uma experiência de usuário fluida enquanto mantém altos padrões de segurança.

### 2. Fluxos de Autenticação

#### 2.1 Registro de Usuários

1. O cliente envia uma requisição POST para `/api/auth/register` contendo:
   - Email (validado quanto ao formato)
   - Senha (mínimo de 6 caracteres)
   - Nome (mínimo de 3 caracteres)

2. O sistema realiza as validações:
   - Verifica se o email já está cadastrado
   - Se o email existir mas não estiver verificado, atualiza os dados
   - Caso contrário, cria um novo usuário

3. A senha é armazenada de forma segura com hash BCrypt (nunca em texto plano)

4. Um token de verificação único é gerado e armazenado junto com um prazo de validade de 24 horas

5. Um evento `UserRegisteredEvent` é disparado, resultando no envio de um email de verificação contendo um link com o token

6. O usuário recebe uma resposta de sucesso com instruções para verificar o email

#### 2.2 Verificação de Email

1. O usuário clica no link recebido por email, que o direciona para `/api/auth/verify?token=xxx`

2. O sistema:
   - Localiza o usuário associado ao token
   - Verifica se o token não expirou (< 24 horas)
   - Marca o email como verificado
   - Remove o token de verificação
   - Notifica o usuário sobre o sucesso da verificação

3. Após a verificação, o usuário pode realizar login

#### 2.3 Login

1. O usuário envia uma requisição POST para `/api/auth/login` com:
   - Email
   - Senha

2. O sistema:
   - Valida as credenciais usando BCrypt para comparar a senha
   - Verifica se o email está verificado
   - Confirma que a conta está ativa e não bloqueada

3. Em caso de sucesso:
   - Atualiza o registro do último login
   - Gera um token JWT de acesso (duração: 30 minutos)
   - Cria um refresh token (duração configurável, tipicamente longa)
   - Retorna ambos os tokens

4. Em caso de falha:
   - Retorna uma mensagem de erro apropriada (credenciais inválidas, email não verificado, conta bloqueada)

#### 2.4 Refresh de Token

1. Quando o token de acesso expira, o cliente envia o refresh token para `/api/auth/refresh`

2. O sistema:
   - Valida o refresh token
   - Verifica se não está expirado
   - Gera um novo token JWT de acesso

3. Retorna o novo token de acesso junto com o mesmo refresh token

#### 2.5 Logout

1. O cliente envia uma requisição POST para `/api/auth/logout` com o token JWT no cabeçalho

2. O sistema:
   - Adiciona o token à blacklist no Redis
   - Revoga todos os refresh tokens do usuário
   - Confirma o logout bem-sucedido

### 3. Tokens JWT

#### 3.1 Estrutura

Os tokens JWT contêm:

- **Subject (sub)**: Email do usuário
- **Issued At (iat)**: Momento da emissão
- **Expiration (exp)**: Momento de expiração
- **Claims personalizadas**: 
  - `tokenType`: Identifica o tipo de token (ACCESS)

#### 3.2 Ciclo de Vida

- **Token de Acesso**: Validade curta (30 minutos) para minimizar riscos
- **Refresh Token**: Validade longa (configurável) para manter a sessão
- **Blacklist**: Tokens revogados são adicionados ao Redis com TTL igual ao tempo restante de validade

### 4. Estratégias de Segurança

#### 4.1 Hashing de Senhas

- **Algoritmo**: BCrypt com salt único para cada senha
- **Implementação**: Spring Security BCryptPasswordEncoder
- **Vantagens**:
  - Resistente a ataques de força bruta
  - Salt único para cada senha
  - Proteção contra ataques de rainbow table

#### 4.2 Proteção Contra Ataques

##### 4.2.1 Rate Limiting

Implementado com Bucket4j para limitar o número de requisições:

- **Endpoints de autenticação** (login, registro): 5 requisições por minuto por IP
- **Endpoints gerais**: 10 requisições por minuto por IP
- **Implementação**: Através de um filtro de servlet (`RateLimitFilter`)
- **Resposta**: Erro 429 (Too Many Requests) quando o limite é excedido

##### 4.2.2 Proteção Contra CSRF

- Configuração stateless com tokens JWT elimina a necessidade de CSRF tokens
- Ausência de cookies de sessão reduz a superfície de ataque

##### 4.2.3 Mitigação de Ataques de Força Bruta

- Rate limiting nos endpoints de autenticação
- Respostas genéricas que não revelam se um email existe ou não

#### 4.3 Blacklisting de Tokens

- Tokens revogados (após logout) são adicionados ao Redis
- TTL baseado no tempo restante de validade do token
- Verificação em todas as requisições autenticadas

#### 4.4 Refresh Tokens

- Armazenados no banco de dados
- Um usuário pode ter múltiplos tokens (login em diferentes dispositivos)
- Todos os tokens são revogados no logout
- Implementação de expiração automática

### 5. Fluxo de Processamento de Requisições Autenticadas

1. A requisição chega com um token JWT no cabeçalho Authorization
2. O `RateLimitFilter` verifica se o IP não excedeu o limite de requisições
3. O `JwtAuthenticationFilter` extrai e valida o token:
   - Verifica a assinatura do token
   - Confirma que não está expirado
   - Consulta a blacklist no Redis
   - Extrai o email do usuário
4. O `UserDetailsService` carrega os detalhes do usuário
5. A autenticação é configurada no `SecurityContext`
6. A requisição é processada pelos controllers protegidos
7. As permissões baseadas em roles são verificadas quando necessário

### 6. Possíveis Melhorias de Segurança

1. **Autenticação de Dois Fatores (2FA)**:
   - Implementação de TOTP (Time-based One-Time Password)
   - Integração com aplicativos como Google Authenticator
   - Códigos de backup para recuperação

2. **Controle de Acesso Baseado em Roles mais Granular**:
   - Permissões específicas para funcionalidades
   - Gerenciamento de grupos de usuários
   - Herança de permissões

3. **Monitoramento Avançado**:
   - Detecção de padrões suspeitos de autenticação
   - Alertas de segurança para tentativas de acesso incomuns
   - Auditorias de login completas

4. **Rotação Automática de Tokens**:
   - Renovação periódica de refresh tokens
   - Implementação de tokens de uso único