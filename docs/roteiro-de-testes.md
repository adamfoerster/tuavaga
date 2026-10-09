# Roteiro de testes manuais — TuaVaga

Roteiro para a equipe de testadores. Cada caso diz o que fazer e o que deve acontecer; marque o
resultado na planilha de execução (modelo na seção 9) e registre qualquer divergência como defeito
(modelo na seção 8).

Versão do app coberta: a de `app.version` em [gradle.properties](../gradle.properties) (confira a
versão instalada em cada plataforma e anote-a no relatório).

## 1. Antes de começar

### 1.1 Plataformas

| Plataforma | Como obter | Cobertura mínima |
| --- | --- | --- |
| Android | APK de debug (`./gradlew :androidApp:installDebug`) | 1 aparelho físico + 1 emulador, Android 10 ou superior |
| Web | `./gradlew :app:wasmJsBrowserDevelopmentRun` → http://localhost:8080, ou o site publicado (HTTPS) | Chrome e Firefox atuais; Safari se possível |
| iOS | Build pelo Xcode (só em Mac) | 1 iPhone, iOS 16 ou superior |

Rode o roteiro completo em **uma** plataforma por rodada e os casos marcados com 📱 (comportamento
do aparelho) também no Android e no iOS. Os demais casos só precisam ser repetidos em outra
plataforma quando o desenvolvimento avisar que a mudança afeta aquele alvo.

### 1.2 Contas e dados de teste

Os testes envolvem **duas partes** (quem aluga e quem anuncia a vaga), então use no mínimo
**duas contas** em **dois aparelhos/navegadores** ao mesmo tempo (ex.: Android + Web, ou duas janelas
anônimas — no navegador anônimo o banco local cai para memória, veja o caso TRV-12).

| Papel | Conta | Observação |
| --- | --- | --- |
| Locador (L) | `teste.locador+<n>@<domínio de teste>` | dono das vagas |
| Locatário (M) | `teste.morador+<n>@<domínio de teste>` | quem pede reserva |
| Estranho (E) | terceira conta, **sem** vínculo com o condomínio de L e M | só para casos de acesso negado |

- O cadastro exige e-mail real que receba o **código de 8 dígitos**. Use um e-mail com aliases
  (`+1`, `+2`…) ou uma caixa de teste da equipe.
- Peça ao desenvolvimento o **ambiente de teste do Supabase** (nunca teste no de produção) e confirme
  que os templates de e-mail trazem o código (`{{ .Token }}`).
- Placas de teste válidas: Mercosul `ABC1D23`; modelo antigo `ABC1234`. Invente placas diferentes para
  cada conta (a placa é única por dono).
- Para os casos de horário, use sempre o **fuso de Brasília (UTC−3)**, que é o do app, qualquer que
  seja o fuso do aparelho.

### 1.3 Preparação do cenário base

Faça uma vez e reaproveite (ver casos CON-xx):

1. L cria o condomínio **"Edifício Teste A"** com 2 andares ("Subsolo 1", "Subsolo 2"), setores
   ("Setor A", "Setor B") e blocos ("Torre 1", "Torre 2"). Anote o **código de convite** (`XX-XXXX`).
2. M entra nesse condomínio pelo código de convite.
3. L anuncia duas vagas: uma com **aprovação automática** e outra com **aprovação manual** (ver HOS-xx).

### 1.4 Regras de ouro

- Teste o **fluxo feliz** e depois o **erro**: campo vazio, valor inválido, sem internet.
- Todo estado deve ser comunicado **por palavra**, não só por cor.
- Textos de interface ficam em português; display e rótulos em **caixa alta**.
- Nomes abreviados ("Marina R.") nunca terminam a frase com ponto duplicado ("Marina R..").
- Qualquer travamento, tela em branco, texto cortado, botão que não responde ou dado errado é defeito.

## 2. Autenticação e introdução

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| INT-01 | Introdução na primeira abertura | Instale o app (ou limpe os dados) e abra | Aparece "Como funciona" antes do login, com "Pular" e "Continuar" |
| INT-02 | Introdução só uma vez | Conclua a introdução, feche e abra o app | A introdução **não** reaparece; vai direto ao login |
| INT-03 | Pular | Em instalação limpa, toque em "Pular" | Vai ao login; não reaparece depois |
| AUT-01 | Cadastro com sucesso | "Criar conta" → e-mail e senha válidos → enviar | Pede o código de confirmação |
| AUT-02 | Validação do cadastro | Tente e-mail inválido (`abc`), senha com menos de 6 caracteres, campos vazios | Mensagem de erro clara em cada campo; nada é enviado |
| AUT-03 | Confirmar e-mail | Digite o código de 8 dígitos recebido | Conta confirmada e usuário logado |
| AUT-04 | Código inválido | Digite 7 dígitos, letras, e um código errado de 8 dígitos | Erro explicando o problema; permite tentar de novo |
| AUT-05 | E-mail já cadastrado | Cadastre de novo o mesmo e-mail | Mensagem de erro; sem travar |
| AUT-06 | Login | Entre com e-mail e senha corretos | Entra no app |
| AUT-07 | Login inválido | Senha errada; e-mail inexistente | Mensagem de erro; permanece no login |
| AUT-08 | Recuperar senha | "Esqueci a senha" → e-mail → código de 8 dígitos → nova senha | Senha trocada; login com a nova senha funciona e a antiga não |
| AUT-09 | Sessão persiste | Logado, feche e abra o app (na web, recarregue a página) | Continua logado, sem pedir senha |
| AUT-10 | 📱 Bloquear a tela no meio do fluxo | Preencha metade de um formulário, bloqueie a tela por ~30 s e volte | O que foi digitado continua lá; a tela **não** volta ao início |
| AUT-11 | Sair da conta | Perfil → "Sair da conta" | Volta ao login; ao entrar de novo, os dados são os da conta |
| AUT-12 | Sem conexão no login | Desligue a internet e tente entrar | Erro de conexão compreensível; sem travar |

## 3. Condomínio e cadastro do morador

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| CON-01 | Estado inicial sem condomínio | Conta nova, logada | Cai no fluxo "Entrar no condomínio" |
| CON-02 | Buscar condomínio | Busque por nome e por parte do endereço | Lista com nome, endereço, torres e vagas anunciadas; sem expor moradores nem o código |
| CON-03 | Buscar sem resultado | Busque um texto inexistente | Estado vazio claro, com caminho para "Cadastrar meu condomínio" |
| CON-04 | Entrar por código de convite | Digite o código `XX-XXXX` correto | Mostra a prévia do condomínio e segue para "Seus dados" |
| CON-05 | Código de convite inválido | Formato errado (`123`) e código no formato certo mas inexistente | Erros diferentes: "formato" e "não encontrado" |
| CON-06 | Seus dados | Preencha nome, bloco, unidade, telefone, vínculo (morador/trabalho) | Valida campos obrigatórios; sem aceitar os termos não conclui |
| CON-07 | Veículo no cadastro | Cadastre placa Mercosul e placa antiga, modelo, cor, tipo | Aceita `ABC1D23` e `ABC1234`; rejeita `AB12`, `ABCD123` e vazio |
| CON-08 | Concluir entrada | Aceite os termos e conclua | Chega à tela principal com o condomínio ativo no topo |
| CON-09 | Cadastrar meu condomínio | Nome, endereço, CEP, andares, setores, blocos/torres | Validação dos campos; reordenar andares com "↑" funciona |
| CON-10 | Condomínio só nasce no fim | Cadastre o condomínio e abandone antes de "Seus dados" | O condomínio **não** é criado (busque-o com outra conta) |
| CON-11 | Código de convite do criador | Após criar, abra o Explorar vazio | Mostra "Seja o primeiro" com o código de convite |
| CON-12 | Trocar condomínio ativo | Seletor no topo → escolha outro condomínio | Todas as abas passam a mostrar os dados do condomínio escolhido |
| CON-13 | Adicionar outro condomínio | Seletor → "Adicionar outro condomínio" | Abre o fluxo de entrar/criar e, ao concluir, o novo aparece na lista |
| CON-14 | Abrir offline | Com o app já usado, desligue a internet e abra | Abre com os condomínios salvos; sem tela de erro |
| CON-15 | Sem conexão no primeiro acesso | Conta nova, internet desligada | Tela "Sem conexão" com como tentar de novo; funciona ao reconectar |

## 4. Locador — vagas

Use a conta **L**.

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| HOS-01 | Aba vazia | "Minhas vagas" sem vagas | Estado vazio com "Anunciar neste condomínio" |
| HOS-02 | Passo 1 — localização | Andar, setor, número, tamanho, descrição, características (coberta, larga, elétrica, elevador, moto), pé-direito, "como chegar" | Progresso "Passo 1 de 3"; sem número não avança |
| HOS-03 | Número duplicado | Cadastre outra vaga com o mesmo andar/setor/número | Erro de duplicidade |
| HOS-04 | Passo 2 — preço e regras | Preço por hora/dia/semana, período mínimo, prazo de cancelamento, aprovação, regras | Sem nenhum preço, não avança; valores em R$ com vírgula |
| HOS-05 | Passo 3 — disponibilidade | Frequência semanal, "Das/Às", dias da semana, calendário | Horário final antes do inicial é recusado |
| HOS-06 | Liberar/bloquear dias | Selecione dias no calendário → "Liberar N dias" / "Bloquear N dias" | O contador acompanha a seleção; estados livre, fechado, passado, selecionado e bloqueado distinguíveis também por texto/legenda |
| HOS-07 | Publicar | Conclua o passo 3 | A vaga aparece em "Minhas vagas" agrupada por condomínio, com a tag de estado |
| HOS-08 | Editar | Toque no cartão da vaga | Abre a edição com tudo preenchido; salvar atualiza a lista |
| HOS-09 | Pausar e reativar | "Pausar" na vaga ativa; depois reativar | Vaga pausada **some** do Explorar de M e volta ao reativar |
| HOS-10 | "Anunciar minha vaga" | Do Explorar, toque em "Anunciar minha vaga" | Abre o cadastro já no condomínio ativo |
| HOS-11 | Ganhos e reservas do mês | Após reservas confirmadas | "Ganhos do mês" e "Reservas" batem com a soma do mês |
| HOS-12 | Agenda da vaga | Cartão → "Agenda" | Calendário do mês e lista das reservas; dias reservados marcados |
| HOS-13 | Solicitações aguardando | Com pedidos pendentes | Botão "N solicitações aguardando" com o número certo e abre a lista |

## 5. Explorar e pedir reserva

Use a conta **M** (com o cenário base pronto).

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| EXP-01 | Lista de vagas | Abra o Explorar | Período padrão = próxima hora cheia por 2 h; lista só com vagas livres no período |
| EXP-02 | Mudar período | Toque em Entrada/Saída; escolha dia (até 60 dias à frente) e hora (de 30 em 30 min) | A lista é recalculada; saída antes da entrada é tratada com mensagem |
| EXP-03 | Filtros | Coberta, larga, elétrica, elevador, moto, "até R$ 10/h" (um a um e combinados) | Resultados coerentes com cada filtro |
| EXP-04 | Mapa da garagem | Alterne Lista/Mapa; troque de andar | Vagas livre, ocupada, selecionada e "sua vaga" distintas (legenda visível) |
| EXP-05 | Nenhuma vaga livre | Escolha um período em que todas estejam ocupadas | Estado "Nenhuma vaga livre" |
| EXP-06 | Sem conexão | Desligue a internet no Explorar | Estado de sem conexão com tentar de novo |
| EXP-07 | Detalhe da vaga | Toque numa vaga | Locador (nome abreviado), preços, dimensões, pé-direito, como chegar, regras numeradas, calendário do mês e valor do período |
| EXP-08 | Vaga própria | Com a conta L, abra o Explorar | A própria vaga aparece como "sua vaga" e **não** dá para reservá-la |
| EXP-09 | Pedido — passo 1 | "Solicitar reserva": forma de cobrança, período, veículo, observação, aceite das regras | Sem aceitar as regras, não avança; só aparecem formas de cobrança oferecidas |
| EXP-10 | Pedido — resumo e envio | Passo 2 → "Enviar solicitação" | Aprovação manual: "Pedido enviado"; automática: "Reserva confirmada"; ambos com o código ("Reserva 10xx") |
| EXP-11 | Valor | Confira o total em vários períodos | Arredonda **para cima**: ex.: 34 h na diária = 2 diárias; igual no resumo e no detalhe da reserva |
| EXP-12 | Período abaixo do mínimo | Peça menos que o mínimo da vaga | Erro de período mínimo |
| EXP-13 | Período já ocupado | Peça um período já reservado/confirmado | Erro de vaga indisponível; sem criar reserva |
| EXP-14 | Pedido sem veículo | Conta sem veículo pede reserva | Permitido (o veículo é opcional) |
| EXP-15 | "Ver reserva" | Ao final do pedido, toque em "Ver reserva" | Abre o detalhe da reserva criada |
| EXP-16 | Troca de condomínio recarrega | Troque o condomínio ativo no Explorar | A lista é recarregada para o outro condomínio |

## 6. Ciclo da reserva

Dois aparelhos: **M** pede, **L** responde. Deixe os dois logados para ver o tempo real.

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| RES-01 | Abas de Reservas | Abra Reservas | Próximas, Em curso e Histórico; cartões com status em palavra |
| RES-02 | Pedido chega ao locador | M envia pedido em vaga manual | L vê o pedido em solicitações (e a notificação) **sem recarregar a tela** |
| RES-03 | Aceitar | L aceita | M vê "confirmada" sem recarregar; chega aviso a M |
| RES-04 | Recusar | L recusa escolhendo cada motivo (visita, uso, veículo, outro) + mensagem | M vê "recusada" com o motivo e a mensagem |
| RES-05 | Conflito de horário | Dois pedidos pendentes no mesmo horário; L aceita um | Aviso de "Conflito de horário" no outro; aceitar o segundo falha com mensagem de conflito |
| RES-06 | Pedido expira | Pedido manual sem resposta por mais de 12 h | Vira "sem resposta"/expirado para os dois lados |
| RES-07 | Detalhe da reserva | Abra uma reserva confirmada | Entrada, saída, "Começa em", como chegar, locador, valor e prazo de cancelamento sem aviso |
| RES-08 | Cancelar dentro do prazo | M cancela antes do prazo da vaga | Cancelada; L é avisado |
| RES-09 | Cancelar fora do prazo | M tenta cancelar depois do prazo | Bloqueado com explicação do prazo |
| RES-10 | Locador cancela | L cancela uma reserva confirmada antes da entrada | M vê "cancelada pelo locador" |
| RES-11 | Check-in antes da hora | M abre o check-in mais de 30 min antes da entrada | Bloqueado com mensagem; liberado a partir de 30 min antes |
| RES-12 | Check-in | M faz check-in marcando as três confirmações | Reserva passa a "Em curso"; mensagem de sistema "CHECK-IN · hh:mm" no chat |
| RES-13 | Check-out | M faz check-out | Reserva vai ao Histórico como concluída |
| RES-14 | Preciso de mais tempo | M, em curso, estende a saída (de 30 em 30 min) | Mostra o novo valor; ao confirmar, a saída muda; limite de até 7 dias; trecho ocupado é recusado |
| RES-15 | Atraso | Passe do horário de saída sem check-out | "Passou do horário" em vermelho/por texto; L recebe aviso de atraso |
| RES-16 | Lembrete de véspera | Reserva confirmada para amanhã | M recebe o lembrete "check-in libera amanhã" |
| RES-17 | Acesso negado | E (estranho) tenta abrir uma reserva de L e M (link/ID) | Não vê nada; sem expor dados |
| RES-18 | Offline | Abra Reservas sem internet | Mostra as reservas salvas ("continuam visíveis") |

## 7. Mensagens, notificações e perfil

### 7.1 Mensagens e notificações (tempo real)

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| MSG-01 | Lista de conversas | Aba Mensagens | Uma conversa por reserva com última mensagem e não lidas |
| MSG-02 | Enviar mensagem | M escreve e envia | Aparece na hora para M e chega a L em segundos, sem recarregar |
| MSG-03 | Respostas rápidas | Toque em "Cheguei", "Pode liberar a vaga?", "Saindo agora", "Preciso de mais 30 min" | Cada uma é enviada imediatamente |
| MSG-04 | Falha de envio | Desligue a internet e envie | Mensagem marcada como não enviada, com "Tentar de novo"; ao reconectar e tentar, é enviada **uma vez** |
| MSG-05 | Não lidas | L recebe mensagem e abre o chat | Contador da aba some depois de ler |
| MSG-06 | Observação do pedido | M escreve observação no pedido | Vira a primeira mensagem do chat |
| MSG-07 | Atalhos | "Mensagem" no detalhe da reserva; tocar no nome no chat | Abre o chat; e do chat abre a reserva |
| MSG-08 | Mensagens de sistema | Confirmar, recusar, cancelar, check-in/out, ajustar saída | Mensagens de sistema com o texto correspondente |
| NOT-01 | Sino com contador | Ao receber um aviso | Sino ao lado do seletor mostra o contador |
| NOT-02 | Central | Abra o sino | Avisos agrupados em Hoje/Ontem; abrir um leva à reserva |
| NOT-03 | Filtro por condomínio | Alterne o filtro | Só avisos do condomínio escolhido |
| NOT-04 | Marcar como lidas | "Marcar como lidas" | Contadores zeram (sino e "N novas" na troca de condomínio) |
| NOT-05 | Reconexão | Desligue a internet por 1 min, ligue de volta | O tempo real volta sozinho e recupera o que perdeu |

### 7.2 Perfil

| ID | Caso | Passos | Resultado esperado |
| --- | --- | --- | --- |
| PER-01 | Cabeçalho | Abra o Perfil | Avatar com iniciais, nome, bloco/unidade do condomínio ativo, total de reservas feitas |
| PER-02 | Adicionar veículo | Folha de veículo: placa, modelo, cor, tipo | Valida a placa; aparece na lista |
| PER-03 | Editar veículo | Altere os dados | Salva e reflete na lista |
| PER-04 | Remover veículo | Remova com confirmação | Some da lista |
| PER-05 | Remover veículo em uso | Remova um veículo de reserva ativa/futura | Recusado com mensagem; o veículo permanece |
| PER-06 | Trocar condomínio ativo | Ative outro condomínio | O topo e todas as abas passam ao outro |
| PER-07 | Sair de condomínio | Saia de um condomínio **não ativo** | Some da lista; vagas dele ficam pausadas |
| PER-08 | Sair do condomínio ativo | Tente sair do ativo | Não há "Sair" nele; é preciso ativar outro antes |
| PER-09 | Sair com reserva ativa | Tente sair de condomínio com reserva ativa | Recusado com mensagem na faixa do rodapé |
| PER-10 | "Em breve" | Preferências de notificação, ajuda, denunciar | Entradas desabilitadas, marcadas "em breve" |
| PER-11 | Excluir conta | "Excluir conta" → confirmação | Pede confirmação; ao confirmar, volta ao login e o login antigo deixa de funcionar |
| PER-12 | Excluir com reserva em curso | Tente excluir com reserva em curso | Recusado com mensagem |
| PER-13 | Excluir cancela reservas futuras | Exclua a conta de M com reserva futura | L recebe o aviso de cancelamento (sem apontar reserva quebrada) |
| PER-14 | Reentrar após excluir | Cadastre-se de novo com o mesmo e-mail | Conta nova, sem dados da antiga |

## 8. Transversais

| ID | Caso | Como verificar | Resultado esperado |
| --- | --- | --- | --- |
| TRV-01 | Tema escuro e claro | Alterne o tema do sistema e percorra as telas | Ambos legíveis; sem texto invisível; contraste adequado |
| TRV-02 | Telas pequenas | Celular de 360×640; navegador a 375 px de largura | Nada cortado ou sobreposto; rótulos longos de botão diminuem para caber |
| TRV-03 | Telas grandes | Tablet e janela larga do navegador | Layout continua utilizável |
| TRV-04 | Rotação 📱 | Gire o aparelho em telas com formulário | Sem perder o que foi digitado |
| TRV-05 | Voltar 📱 | Botão/gesto de voltar em cada tela | Volta à tela anterior lógica; nunca fecha o app no meio de um fluxo sem aviso |
| TRV-06 | Fuso | Aparelho em fuso diferente (ex.: UTC+0) | Horários seguem o de Brasília (UTC−3) |
| TRV-07 | Troca de app 📱 | Vá para outro app e volte no meio de um fluxo | Estado preservado |
| TRV-08 | Dados offline | Internet desligada em cada aba | Nenhum travamento; mensagens de erro compreensíveis |
| TRV-09 | Textos | Revise ortografia e acentuação; textos em caixa alta nos rótulos | Sem erros; sem "Marina R.." |
| TRV-10 | Acessibilidade 📱 | Leitor de tela (TalkBack/VoiceOver) e fonte grande do sistema | Botões e campos anunciados; layout não quebra |
| TRV-11 | Web: recarregar | Recarregue a página em cada tela | Continua logado; sem tela em branco |
| TRV-12 | Web: aba anônima | Use o app em janela anônima | Funciona (banco local em memória); ao fechar a janela, a sessão se perde |
| TRV-13 | Web: HTTPS | Acesse o site publicado | Carrega por HTTPS; sem erro de console bloqueante |
| TRV-14 | Duas contas ao mesmo tempo | Ações simultâneas de L e M | Cada um vê só o que é seu; sem vazamento de dados de outras contas |

## 9. Registro dos resultados

### 9.1 Planilha de execução

Copie para uma planilha (uma linha por caso e por plataforma):

| ID | Plataforma | Versão do app | Testador | Data | Resultado (OK / FALHA / BLOQUEADO / N/A) | Defeito |
| --- | --- | --- | --- | --- | --- | --- |
| AUT-06 | Android 14 | 0.8.2 | Fulana | 2026-10-09 | OK | — |

- **OK**: comportamento igual ao esperado.
- **FALHA**: divergiu; abra um defeito e anote o número.
- **BLOQUEADO**: não deu para executar (ex.: depende de outro caso que falhou).
- **N/A**: o caso não se aplica a essa plataforma.

### 9.2 Modelo de defeito

```
Título:        resumo em uma linha
ID do caso:    ex.: RES-09
Plataforma:    Android 14 / Chrome 130 / iOS 18, versão do app
Contas:        locador/locatário usados (sem informar senhas)
Passos:        1) … 2) … 3) …
Esperado:      …
Obtido:        …
Evidência:     captura de tela ou vídeo; no navegador, o console (F12)
Gravidade:     Crítico (perde dados, trava, vaza dados) / Alto (fluxo principal quebrado)
               / Médio (fluxo alternativo) / Baixo (visual, texto)
Frequência:    sempre / às vezes / uma vez
```

### 9.3 Critérios de aceite da rodada

- Nenhum defeito **Crítico** ou **Alto** aberto.
- 100% dos casos de Autenticação, Explorar/pedido e Ciclo da reserva executados em pelo menos uma
  plataforma, e os 📱 em Android e iOS.
- Todo defeito corrigido foi retestado pelo mesmo caso e por um teste de regressão nas telas vizinhas.
