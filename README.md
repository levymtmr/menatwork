# Menatwork 1.9.0

Aplicativo Android pessoal, em inglês ou português, para acompanhar o ganho líquido estimado de uma jornada de trabalho. Interface de terminal retrofuturista inspirada no Pip-Boy: fósforo verde, linhas sutis de CRT, painéis monocromáticos e trabalhador animado. Ilustrações originais desenhadas em canvas.

## Instalar

O instalador fica em `dist/neon-shift-1.9.0.apk`. Veja `INSTALAR.md`.

Compatível com Android 8.0 ou superior, incluindo o Galaxy S22 Ultra. O APK usa apenas Java e APIs do Android, sem bibliotecas nativas, conta no aplicativo ou servidor de registros. O GitHub hospeda as atualizações públicas. A interface é local em WebView; o GPS, a persistência e o acompanhamento em segundo plano são nativos.

## Cadastro de clientes (1.7.0)

Em **Settings → Clients → New client** (Ajustes → Clientes → Novo cliente), cadastre nome, e-mail, endereço e ABN. Apenas o nome é obrigatório. O cadastro é persistido separadamente dos invoices e jornadas, com identificação estável e edição, busca e seleção. Nomes repetidos (ignorando maiúsculas e espaços) e e-mails inválidos são recusados. O nome aceita até 120 caracteres para corresponder ao modelo do invoice; endereço até 350, e-mail até 160 e ABN até 40.

O botão **＋** ao lado de cada seleção abre o cadastro. Ao salvar, o novo cliente fica selecionado no turno, no editor do histórico ou no invoice de onde você abriu o formulário. É possível cadastrar mesmo antes de existir o primeiro invoice. Na primeira abertura, clientes válidos já existentes são importados de rascunho, invoices e jornadas, sem modificar esses registros. Novos nomes usados em jornadas salvas ou invoices emitidos são lembrados sem sobrescrever contatos já cadastrados.

No invoice, selecionar um cliente preenche apenas nome, endereço, ABN e e-mail do cliente. Prestador, banco, tarifa e lançamentos permanecem. Editar o cadastro não reescreve PDFs emitidos ou nomes históricos. Se o cliente editado estiver no rascunho atual, os campos do cliente nesse rascunho são atualizados; confira antes de emitir.

Versão 1.7.0 / versionCode 10. O PDF continua sempre em inglês.

## Cards e clientes rápidos (1.6.2)

A navegação inferior usa apenas os nomes das abas; Ready/Pronto fica oculto no estado ocioso. Turnos em andamento ainda mostram seu estado.

**Clientes salvos / Saved clients** permite escolher nomes já presentes em jornadas, invoices e no rascunho. No invoice, escolher um cliente conhecido também preenche endereço, ABN e e-mail da fatura anterior, preservando prestador, banco e descrição atual. Clientes sem dados conhecidos recebem esses campos vazios para evitar enviar a cobrança ao contato anterior. Nomes e dados digitados não são traduzidos.

Cards de jornadas, invoices e habilidades abrem recolhidos. Use **Expandir/Recolher** para mostrar os detalhes e ações. Data, cliente, valores e estado de pagamento continuam visíveis no resumo. Os painéis dos Ajustes, extrato, trajeto e rascunho também têm controles para recolher; o estado é mantido durante esta execução do app e quando os dados são atualizados.

Versão 1.6.2 / versionCode 9. O modelo do PDF permanece em inglês.

## Locais salvos e seletores (1.6.1)

Os endereços usados nos shifts são guardados no celular em uma lista independente do histórico. Locais de jornadas antigas também são reconhecidos, e a primeira gravação importa esses endereços. A lista evita duplicatas por maiúsculas/minúsculas e espaços; Day off não é tratado como endereço.

**Locais salvos / Saved work sites** aparece quando existem endereços conhecidos, no ajuste de jornada atual, no editor de um dia e nas linhas do invoice. Escolher uma opção preenche o endereço; é possível completar ou alterar o texto antes de salvar. O app não escolhe um local automaticamente para um novo turno, pois o usuário pode trabalhar em locais diferentes. As sugestões também reconhecem endereços já presentes em invoices locais.

Seletores agora preservam a escolha durante a atualização automática. A moeda nos Ajustes reconhece o evento `change` do Android, mesmo quando não há `input`; salve os Ajustes para persistir a alteração. Opções de filtros são atualizadas sem reconstruir a lista enquanto ela está aberta. Os valores internos das opções são preservados durante a tradução.

Versão 1.6.1 / versionCode 8, com o mesmo pacote e certificado.

## Almoço, filtros e Retro Console (1.6.0)

**Encerrar** pergunta se houve almoço não pago. Escolha Sim ou Não; ao escolher Sim, o campo sugere 45 minutos, que podem ser alterados. O fim do trabalho é capturado ao abrir a pergunta; responder não acrescenta tempo pago. Cancelar mantém a jornada ativa. Na opção de registrar a volta, o almoço é informado ao salvar a jornada, usando apenas as horas de trabalho já encerradas.

O app registra o tempo anterior ao almoço e subtrai os minutos não pagos uma vez. Assim, 8h − 45 min = 7h15 pagas; a AUD 32/h, o bruto é AUD 232. Ganhos, XP e horas levadas ao invoice usam o tempo pago. O almoço não vira uma segunda despesa em dinheiro. Se já pausou ou ajustou o tempo para excluir o almoço, informe apenas minutos ainda não descontados ou escolha Não. Em jornadas manuais, informe horas antes do almoço ou início/término, outras pausas e os minutos de almoço separadamente. Editar e salvar novamente não repete o desconto.

**Histórico** e **invoices** têm busca, esta semana/semana passada (segunda a domingo), este mês ou datas personalizadas, cliente, tipo de trabalho, situação e ordenação. O período do histórico usa a data do trabalho; o de invoices usa a data de emissão. Os resumos correspondem aos registros filtrados. O botão CSV continua exportando todas as jornadas válidas. As listas mostram inicialmente 20 registros e carregam mais ao rolar, com botão alternativo. Lixeiras também carregam em blocos.

Cliente e tipo podem ser informados em **Hoje → Ajustar jornada e custos** ou ao criar/editar um dia. Para registros anteriores, os filtros também reconhecem clientes de invoices vinculados e habilidades já atribuídas. O cliente do invoice continua sendo o perfil escolhido no rascunho; o campo de classificação da jornada não substitui os dados do documento.

O novo padrão **Retro Console** segue a direção de instrumentos retrofuturistas da referência indicada pelo usuário: blocos creme, coral e âmbar, bordas arredondadas e mostrador circular desenhado em canvas. Não incorpora imagens de terceiros. As escolhas explícitas de tema já salvas são preservadas; o novo padrão vale quando não há escolha salva. Pode selecioná-lo a qualquer momento em Settings → Themes. O PDF continua em inglês e mantém o modelo original.

Versão 1.6.0 / versionCode 7. Testes de filtros em Node podem usar `NODE_BIN` para indicar o executável local.

## Temas e pagamentos (1.5.0)

**Ajustes → Temas** alterna entre Pip-Boy, Medieval pixel art e Cyberpunk. Cada tema muda cores, painéis e a ilustração animada do trabalhador. A escolha é persistida separadamente dos valores de trabalho. O PDF permanece branco no padrão Invoice Deck. Telas e instruções foram compactadas para dar espaço aos controles e registros.

O Histórico deriva as marcações dos vínculos atuais: **Adicionada ao rascunho**, **Adicionada · Invoice #…** e **Paga · Invoice #…**. Retirar a linha do rascunho ou cancelar o invoice remove a marcação correspondente. Corrigir um invoice transfere os vínculos para o novo rascunho, e uma jornada removida desse rascunho fica disponível para nova cobrança. Marcas não são gravadas nas jornadas; por isso acompanham as mudanças dos invoices sem ficar desatualizadas.

Em **Invoice → Marcar como pago**, o pagamento é registrado manualmente no celular. **Marcar como não pago** desfaz a marcação. As jornadas de um invoice ativo pago também ficam marcadas como pagas. O resumo separa pagos e a receber; invoices cancelados ficam fora desses valores. Invoices que precisam de revisão devem ser corrigidos antes de marcar o pagamento. Marcar pagamento não altera XP, custos ou PDFs, não movimenta dinheiro e não envia mensagens ao cliente.

A interface usa inglês por padrão. **Settings → App language** permite escolher português; **Ajustes → Idioma do aplicativo** permite voltar ao inglês. A preferência é persistida independentemente do tema. Rótulos, controles, datas/números e diálogos acompanham a escolha. Conteúdos digitados pelo usuário são preservados. O modelo de PDF e a mensagem de envio permanecem sempre em inglês, independentemente do idioma da interface.

Versão 1.5.0 / versionCode 6, com o mesmo pacote e certificado das instalações anteriores.

## Jornadas, invoices e habilidades (1.4.0)

- **Histórico → Adicionar dia de trabalho** registra dias esquecidos. Informe horas pagas (8,5 ou 08:30) ou início/término com pausas e opção de terminar no dia seguinte. Datas futuras e horários de término futuros são recusados; cada lançamento aceita até 24 horas pagas.
- **Editar** altera data, horas, custos, local, observações e habilidades. O registro é substituído, sem duplicar valores. O modo de horas totais não representa horários reais de entrada/saída.
- **Hoje → Corrigir horários / encerrar** permite terminar um cronômetro esquecido usando o horário correto. **Descartar jornada atual** envia o registro para a lixeira.
- Jornadas removidas saem do acumulado diário, resumo do histórico, CSV e XP. A lixeira permite restaurar. O histórico mantém IDs estáveis quando você altera uma data.
- Invoices podem ser removidos dos totais e restaurados. Números usados continuam reservados e o PDF original é preservado. A restauração bloqueia cobrança duplicada de uma jornada já usada em outro invoice ativo.
- **Corrigir / novo rascunho** cancela o invoice anterior localmente e cria um rascunho com novo número, horas e datas atualizadas. Jornadas removidas saem desse rascunho. Confira e emita o novo PDF. Um PDF já enviado ao cliente não é alterado ou retirado automaticamente.
- Alterar data, horas ou local de uma jornada vinculada sinaliza o invoice para revisão e bloqueia seu compartilhamento pelo app. O rascunho tem **Atualizar jornadas vinculadas** para importar os ajustes e retirar jornadas removidas.
- **Skills** permite criar, editar, remover e restaurar habilidades. Marque-as durante o turno ou no editor de um dia. São **100 XP por hora paga**, usando as horas reais, sem arredondamento do invoice, e **1.000 XP por nível**. Frações de XP são acumuladas em tempo antes de truncar para pontos inteiros. O XP geral conta o tempo uma vez; cada habilidade marcada recebe as mesmas horas. Remover uma habilidade não remove o trabalho nem o XP geral.
- XP da jornada em andamento é provisório. Ao corrigir/remover/restaurar registros, a experiência é recalculada a partir dos dados válidos, sem bônus duplicados.

A interface verde inspirada em terminais retrofuturistas continua, com uma quinta aba e editor rolável com botão de salvar fixo. O modelo profissional branco do invoice permanece igual.

Versão 1.4.0 / versionCode 5, assinada pela mesma chave. Instale por **Ajustes → Atualizar aplicativo**, preservando os dados. O nome visível agora é Menatwork. O pacote `com.neonshift.app`, preferências internas e nomes de APK `neon-shift-X.Y.Z.apk` foram mantidos para compatibilidade com as instalações e atualizadores anteriores.

## Atualizações e GitHub

Repositório oficial: https://github.com/levymtmr/menatwork

Baixe a primeira instalação/atualização pela página de Releases. Depois use **Ajustes → Atualizar aplicativo**. O app consulta `releases/latest/download/update.json`, compara o versionCode, baixa o APK via HTTPS e verifica tamanho, SHA-256, package ID, versão, compatibilidade e assinatura igual à instalada. O instalador do Android é aberto automaticamente ao terminar o download iniciado pelo botão. A autorização de instalar a partir do Menatwork e a confirmação final são feitas pelo usuário nas telas do sistema. Não há instalação silenciosa nem consulta em segundo plano sem tocar no botão.

O download não exige conta GitHub: o repositório e as Releases são públicos. A WebView da interface continua sem acesso à rede; somente o atualizador nativo faz essas consultas. Não são enviados registros, dados de invoice ou localização. Se houver falha de conexão, o botão permite tentar novamente. Um download já validado pode ser retomado na etapa de instalação após reabrir o app. Um download interrompido recomeça ao tentar novamente.

A versão 1.2.0 usa versionCode 3, com o mesmo certificado das versões anteriores. Instale por cima delas, sem desinstalar. Atualizações mantêm os dados privados do aplicativo. As instalações novas vêm com campos de exemplo, sem dados reais de prestador, banco, cliente ou obras; preencha no celular. Os dados que já foram salvos no rascunho ou histórico permanecem. O número inicial 018 e a reserva 001–017 são apenas a referência de sequência deste projeto e podem ser ajustados ao criar uma fatura.

### Publicar uma próxima versão

1. Aumente `versionCode` e altere `versionName` em `version.properties`.
2. Atualize `RELEASE_NOTES.md`; rode os testes e `scripts/build-apk.sh` com o SDK/JDK configurados.
3. Preserve a chave privada local em `.tools/neonshift-local.jks`. Ela não está no GitHub. A assinatura deve corresponder ao certificado público em `release-signing-cert.sha256`.
4. Execute `python3 scripts/prepare-release.py --notes-file RELEASE_NOTES.md`. O script confere assinatura e versão e gera `dist/update.json` com hash e URL exata do APK.
5. Faça commit, envie a branch e publique a tag `vX.Y.Z` no GitHub.
6. Publique uma Release estável, marcada como latest, contendo o APK, seu checksum e `update.json`. Pode usar `scripts/publish-release.sh` com GitHub CLI autenticado ou enviar os três arquivos pela interface de Releases. Nunca publique a chave de assinatura.

O workflow Android faz testes e compilação com chave temporária. Ele não tem a chave de produção e não publica updates. APKs de CI ou de outra chave não atualizam a instalação original. A publicação das versões oficiais usa a chave privada preservada no computador do desenvolvedor.

## Funcionalidades da versão 1.1.0

- Barra inferior fixa fora da área rolável. O Android reserva a área de navegação, status, recortes e teclado no `FrameLayout` nativo; os insets são consumidos antes de chegar à WebView, evitando sobreposição ou margem duplicada.
- Personagem em pixel art com três poses de martelada durante o trabalho; fica parado na pausa. A cidade mantém chuva e neon. O carrinho também saiu da ilustração do trajeto.
- Aba **Invoice** com perfil do prestador, cliente, pagamento, rascunho persistido, edição de lançamentos e PDF no padrão do Invoice Deck.
- Jornadas finalizadas podem ser incluídas pelo histórico. O local da obra pode ser registrado nos ajustes de km/horas ou completado no invoice.
- Emissão preserva o PDF, usa numeração única local e bloqueia nova cobrança da mesma jornada vinculada. Prévia não emite. Cada PDF pode ser aberto, salvo pelo seletor de arquivos ou compartilhado como anexo por um content provider com acesso temporário de leitura.

Instale a atualização por cima da versão anterior, sem desinstalar. O package ID e o certificado são os mesmos, e o versionCode passou de 1 para 2. Assim, os registros e ajustes existentes são preservados.

## Invoice Deck no celular

O modelo segue o programa Invoice Deck: PDF A4 branco em inglês, título INVOICE FOR SERVICES, FROM, BILL TO, WORK DETAILS, tabela **Date / day | Work location | Amount AUD**, total, pagamento e rodapé numerado. Horas, tarifa, imposto, reserva e custos do carro não aparecem no PDF.

A regra do programa original foi preservada com `BigDecimal`: arredondar cada lançamento para cima em blocos de 15 minutos, multiplicar pela tarifa do invoice e arredondar cada valor para centavos com HALF_UP. Depois, somar os valores das linhas. Ex.: 08:01 × AUD 32/h = 8,25 h cobradas = AUD 264,00. Day off aparece com zero e não soma horas; linhas comuns zeradas ficam fora. Cada lançamento aceita até 24 horas.

A tarifa da fatura é independente do painel de custos e deve ser conferida. Os invoices são em AUD; não existe conversão cambial. Uma jornada em outra moeda não é importada automaticamente para cobrança.

Os dados iniciais públicos são exemplos. Preencha prestador, cliente e banco dentro do aplicativo; perfis reais já salvos são preservados nas atualizações. A referência de numeração era 018, após o 017 no computador. Não há sincronização com o Invoice Deck: os números usados até a cópia são reservados, mas novas emissões feitas no computador não aparecem automaticamente no celular. Confira a numeração antes de emitir e altere o número se necessário. Essa cópia não modifica o banco de dados ou os PDFs do programa original.

**Enviar ao cliente** abre o compartilhamento do Android com PDF anexado, destinatário, assunto e mensagem. Escolha seu app de e-mail, revise e envie nele. Não há envio automático, SMTP ou credenciais de e-mail no Menatwork. Abrir o compartilhamento não marca o invoice como enviado. Os PDFs originais ficam preservados; use remoção local ou correção com novo número para ajustar o controle de cobrança.

O rascunho salva meio segundo após mudanças. Ao emitir, os lançamentos são limpos e o número avança; prestador, cliente, pagamento e tarifa permanecem para o próximo documento. Novos lançamentos podem ser digitados manualmente ou adicionados pelo histórico.

## O que está implementado

- Jornada com iniciar, pausar, retomar e encerrar; ganho bruto e líquido atualizados enquanto o app está aberto e em uma notificação durante a jornada.
- Registro de ida e volta por GPS, separado do tempo de trabalho. A origem pode mudar a cada trajeto. Toque em **Cheguei** para começar o tempo pago.
- Correção manual de distância, horas pagas e gastos extras, inclusive pedágio, estacionamento e alimentação.
- Hora de trabalho, moeda, reserva de imposto, consumo, combustível, pneus, manutenção e depreciação configuráveis.
- Valores congelados no início de cada jornada: mudar ajustes não altera um registro em andamento ou histórico.
- Histórico local com detalhamento e exportação CSV pelo seletor de arquivos do Android.
- Acumulado diário por moeda, agrupado pela data local de início da jornada e incluindo a jornada atual.
- Cronômetro monotônico durante a mesma inicialização do celular; recuperação da sessão persistida após encerramento do processo.
- Serviço de localização somente nos trajetos; serviço `specialUse` durante trabalho/pausa, com notificação persistente. Nenhuma permissão de localização “o tempo todo” é solicitada.

## Cálculo

```text
bruto = horas pagas × valor por hora
imposto estimado = bruto × percentual informado / 100
combustível = km × consumo em L/100 km × preço por litro / 100
custos do carro = combustível + km × (pneus/km + manutenção/km + depreciação/km)
líquido = bruto − imposto estimado − custos do carro − gastos extras
```

O líquido pode ser negativo, principalmente durante o trajeto, antes de começar a trabalhar. Cálculos usam a precisão completa; a interface arredonda para centavos. Por isso a soma visual de itens arredondados pode diferir em um centavo do resultado.

Os padrões são exemplos: AUD, $ 32/h, 15% de imposto, 8 L/100 km, $ 1,85/L, pneus $ 0,025/km, manutenção $ 0,05/km e depreciação $ 0,10/km. Ajuste antes de iniciar. O imposto é uma reserva percentual configurada pelo usuário, não um cálculo tributário por faixas.

## Localização e segundo plano

O GPS mede a distância percorrida; não estima uma rota antecipada entre endereços. A imagem do trajeto é ilustrativa, não um mapa real. Não há geocodificação, API de mapas ou consulta automática de preço de combustível.

O registro precisa ser iniciado com o app aberto. Permita localização **precisa** e notificações. A amostragem solicita posições a cada 3 segundos, ignora precisão pior que 35 m, posições antigas, saltos acima de 65 m/s e pequenos deslocamentos compatíveis com ruído. Lacunas maiores que 120 segundos não são ligadas em linha reta; o app avisa para conferir e corrigir a distância. Esses filtros reduzem ruído, mas não tornam o celular um odômetro de precisão.

Não há retomada automática após reiniciar o telefone ou forçar a parada. Abra o app e toque em **Retomar acompanhamento**. O tempo da sessão continua pelo horário salvo quando há reinicialização; revise e corrija as horas em **Ajustar km e gastos extras** se o período parado não deve ser pago. A tela continua calculando a sessão mesmo se o Android interromper o serviço, e mostra um aviso de recuperação.

O Samsung pode aplicar limites de bateria; se necessário, escolha **Sem restrições** nas informações de bateria do app. Isso não garante operação sob desligamento, falta de sinal, revogação de permissão ou parada forçada.

## Dados

Não há backend de registros, analytics ou anúncios. A permissão de internet é usada para consultar e baixar atualizações oficiais do GitHub quando solicitado. O envio do PDF é realizado pelo aplicativo de e-mail escolhido por você. Registros e ajustes ficam em SharedPreferences privativas. Invoices emitidos e seus PDFs ficam no armazenamento privado; somente os PDFs escolhidos são expostos a outros apps com permissão temporária de leitura. O perfil distribuído não contém dados pessoais ou bancários reais; configure os campos no celular e confira antes de emitir. A sessão ativa guarda a última posição recebida; coordenadas são removidas ao arquivar a jornada. O histórico conserva distância, horários, custos e configuração. O backup automático está desabilitado. Desinstalar ou limpar dados remove registros e PDFs; exporte CSV e salve cópias dos invoices antes disso. Instale atualizações sem desinstalar. O CSV contém os registros arquivados, não a jornada ainda aberta.

## Compilar

Requer JDK 17/21, Android SDK Platform 35, Build Tools 35.0.0 e os comandos `zip`, `find`, `sed`, `sha256sum`.

```bash
export JAVA_HOME=/caminho/do/jdk
export ANDROID_HOME=/caminho/do/android-sdk
scripts/test.sh
scripts/build-apk.sh
```

A versão do APK é definida por `version.properties`. O script usa `aapt2`, `javac`, `d8`, `zipalign` e `apksigner`, sem baixar dependências Gradle. A assinatura v2/v3 e o alinhamento são verificados ao final. A chave de assinatura fica em `.tools/neonshift-local.jks`, fora do Git e dos pacotes de código. O script local usa a senha de desenvolvimento configurada no projeto. Preserve essa chave em local privado para atualizar o APK sem desinstalar; ela não entra no pacote de código-fonte e não é uma credencial de publicação na Play Store. Para uma distribuição pública, configure e proteja uma identidade de assinatura de produção própria.

Também há arquivos Gradle para abrir no Android Studio (AGP 8.9.2, JDK 17, Gradle 8.11.1). O caminho principal verificado é o script acima; o projeto não inclui um wrapper Gradle.

## Verificação

- Testes Java: 26 verificações de cálculo, custo de trajeto sem tempo pago, resultados negativos, entradas inválidas, precisão e relógio monotônico/recuperação.
- Testes de invoices: 29 verificações de arredondamento por linha, meio centavo, Day off, entradas inválidas e PDFs com uma e seis páginas. As seis páginas foram renderizadas com Poppler e inspecionadas.
- `tests/ui.cjs`: testes Playwright da interface local com ponte Android simulada. Cobre trajeto → trabalho → pausa → retorno → histórico, decimais com vírgula, validação, configuração congelada e exportação; cobre também rascunho, inclusão de jornada, emissão e comandos de compartilhar/salvar PDF. Verifica ausência de erros JavaScript, transbordamento e limites da barra inferior nas quatro páginas, larguras 320, 360, 412 e 480 px e alturas 400 e 858 px.
- Testes da política de atualização: versão monotônica, URLs HTTPS oficiais, tamanho máximo, hash e rejeição de pacote/canal incorretos.
- Compilação real do APK e verificação criptográfica com ferramentas oficiais do Android.
- APK instalado com sucesso no emulador Android 15. A conferência visual nativa ficou limitada por falhas do System UI e do renderer da WebView no emulador sem aceleração; os testes de layout e cliques da interface foram feitos em Chromium. Não houve teste em um S22 Ultra físico; os insets reais, GPS, notificações e políticas de bateria ainda precisam de verificação no aparelho.
- Passaram 19 verificações nativas no Android 15: rascunho persistido, prévia sem emissão, PDF emitido, arredondamento, duplicatas, conteúdo/MIME do anexo, destinatário e provider somente leitura. Nenhum e-mail foi enviado.
- `tests/android/InvoiceSmoke.java` e `scripts/build-device-tests.sh` fornecem verificação nativa do armazenamento, emissão, duplicatas, Intent de anexo e provider. Use somente em emulador descartável: o teste limpa os dados de invoice do dispositivo de teste. Para os testes nativos, compile primeiro `scripts/build-update-fixtures.sh` e depois `scripts/build-device-tests.sh`. As fixtures de versão futura/chave incorreta ficam em `build/` e nunca devem ser publicadas como releases. O APK de testes não faz parte do instalador entregue e não envia e-mail.

Para executar os testes de interface, instale Playwright e Chromium no ambiente de desenvolvimento e rode `node tests/ui.cjs`. Eles não substituem testes de GPS em um aparelho real.

As permissões e os tipos de serviço seguem a [documentação oficial de foreground services](https://developer.android.com/develop/background-work/services/fgs/service-types).

### Tarifa por cliente (1.7.1)

Defina o valor por hora em Ajustes → Clientes → Editar. Ao selecionar o cliente, a tarifa é preenchida na jornada ou no invoice. A troca de cliente no editor da jornada ativa aplica a tarifa ao salvar. Alterar o cadastro não modifica jornadas já registradas nem invoices emitidos.

### Cliente ao iniciar (1.7.2)

Ao iniciar o trajeto ou o trabalho, escolha o cliente na lista com as tarifas. O serviço salva o cliente e uma cópia da tarifa na nova jornada. Settings → Work permite selecionar o cliente e atualizar sua tarifa. Alterações posteriores não recalculam a jornada ativa.

### Overtime e semana (1.8.0)

Ative Overtime em Ajustes → Clientes → Editar. O cliente usa 1× antes das 15h, 1,5× entre 15h e 17h e 2× após 17h no fuso salvo na jornada. Pausas e almoço não recebem adicional. Informe o início do almoço (12:00 vem preenchido e pode ser alterado). Para lançamento manual com overtime, use início/término e o intervalo de almoço; outras pausas devem ser registradas pelo cronômetro ou em jornadas separadas. A tarifa e a regra ficam congeladas na jornada.

O invoice usa a tarifa do registro de overtime, mantém o arredondamento de horas a 15 minutos e acrescenta o prêmio de overtime calculado pelo tempo efetivo. Atualizar as jornadas vinculadas atualiza esse adicional. O PDF continua em inglês.

O painel Esta semana considera todos os dias de segunda a domingo por data de trabalho, soma horas pagas, bruto e líquido, inclui a jornada atual e exclui registros removidos. Moedas ficam separadas. O almoço da jornada ativa é deduzido ao finalizar. Jornadas antigas preservam os valores registrados.

### Marken / SA Recruitment (1.9.0)

A regra `marken` segue as condições fornecidas pelo usuário, sem usar os cortes fixos de 15h/17h. Por data de trabalho e cliente, as primeiras 8 horas de segunda a sexta são normais, as próximas 2 são OT1.5 e o restante OT2. Aos sábados são 2 horas de OT1.5 e o restante OT2; aos domingos tudo é OT2.

Cada turno estritamente acima de 4 horas desconta 30 minutos, uma única vez. Em dias úteis o desconto sai da faixa normal disponível (ou da próxima faixa disponível se o registro já começa após a cota normal); no fim de semana sai de OT2. Não pause o cronômetro para o almoço: informe o período completo e o app aplica o desconto. A data de trabalho do registro define o dia de pagamento; para trabalho atravessando dias com regras diferentes, registre um lançamento para cada data.

Ao corrigir, remover ou restaurar registros Marken, os outros registros Marken do mesmo dia são recalculados para não reiniciar as faixas diárias. O vínculo do invoice acusa mudanças; os PDFs já emitidos permanecem intactos. Configurar o cliente não migra automaticamente jornadas históricas: use Histórico → Editar → Regra Marken.

O invoice Marken usa horas reais pagas, sem teto de 15 minutos, e inclui as faixas e o almoço em inglês. Os gastos pessoais com carro continuam no líquido estimado e não são adicionados ao invoice como auxílio de viagem ou alimentação.

Teste adicional de integração do Store com preferências Android em memória: baixe `org.json:json:20240303` do Maven Central para uma pasta temporária e execute `JSON_TEST_JAR=/caminho/json.jar scripts/test-marken-store.sh`. O CI verifica o SHA-256 desse arquivo e executa o mesmo teste. As classes em `tests/jvm/android` não entram no APK.
