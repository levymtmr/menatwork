# Menatwork 1.4.0

Aplicativo Android pessoal, em português, para acompanhar o ganho líquido estimado de uma jornada de trabalho. Interface de terminal retrofuturista inspirada no Pip-Boy: fósforo verde, linhas sutis de CRT, painéis monocromáticos e trabalhador animado. Ilustrações originais desenhadas em canvas.

## Instalar

O instalador fica em `dist/neon-shift-1.4.0.apk`. Veja `INSTALAR.md`.

Compatível com Android 8.0 ou superior, incluindo o Galaxy S22 Ultra. O APK usa apenas Java e APIs do Android, sem bibliotecas nativas, conta no aplicativo ou servidor de registros. O GitHub hospeda as atualizações públicas. A interface é local em WebView; o GPS, a persistência e o acompanhamento em segundo plano são nativos.

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
