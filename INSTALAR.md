# Instalar / atualizar no Samsung S22 Ultra

Se já usa a versão anterior, instale o novo APK por cima dela e escolha **Atualizar**. Não desinstale: assim seus registros e ajustes são preservados.

1. Copie `neon-shift-1.10.5.apk` para o celular, por cabo USB ou outro meio de sua preferência.
2. Abra o APK pelo aplicativo **Meus Arquivos**. Se o Android pedir, permita instalar aplicativos dessa fonte e volte para concluir a instalação.
3. Abra **Menatwork** e entre em **Ajustes**. Configure sua moeda, hora de trabalho, imposto estimado, consumo do carro, preço da gasolina e custos por km. Os valores iniciais são exemplos.
4. Ao sair para trabalhar, toque em **Iniciar trajeto ao trabalho**. Permita localização precisa e notificações quando solicitado. O GPS funciona melhor ao ar livre.
5. Ao começar a trabalhar, toque em **Cheguei · iniciar trabalho**. O GPS da ida para; começa a contagem de horas pagas. Use **Pausar** para intervalos não remunerados.
6. Para incluir a volta, toque em **Terminar trabalho e registrar volta**. O tempo pago para e o GPS volta a medir. Ao chegar, salve a jornada. Se não quiser registrar a volta, use **Encerrar** diretamente.
7. Antes de salvar, confira **Ajustar km e gastos extras**. Você pode corrigir a distância e as horas pagas, e incluir pedágio, estacionamento ou outros gastos.

O histórico fica apenas no celular. Use **Histórico → Exportar CSV** para guardar uma cópia dos registros finalizados. Desinstalar ou limpar dados apaga o histórico.

Se o Samsung interromper o acompanhamento, abra as configurações do app pelo botão em **Ajustes** e confira as permissões e a opção de bateria **Sem restrições**. Após reiniciar ou forçar a parada, abra novamente o app e retome o acompanhamento. Confira e corrija as horas se necessário.

O combustível é estimado pelo consumo e pelo preço que você informar. O GPS registra o caminho percorrido, sem consulta a mapas ou navegação por endereço. Faça o primeiro teste em um percurso conhecido para conferir as configurações e a distância.

## Criar e enviar invoice

1. Em **Histórico**, use **Adicionar ao invoice** em cada jornada que será cobrada. Ou adicione lançamentos manualmente na aba **Invoice**.
2. Confira data, local da obra e horas de cada lançamento. Você pode registrar o local antecipadamente em **Hoje → Ajustar km e gastos extras**.
3. Na aba **Invoice**, confira prestador, cliente, e-mail e pagamento. Instalações novas usam dados de exemplo. Preencha os dados reais no celular; os dados já salvos de versões anteriores são mantidos.
4. Confira o número e a tarifa em AUD. A numeração e a tarifa do invoice são independentes do computador e dos ajustes do painel. O primeiro número sugerido na cópia é 018; se já usou esse número no computador, altere antes de emitir.
5. O cálculo segue o programa original: cada lançamento sobe para o próximo bloco de 15 minutos. **08:01 a AUD 32/h = AUD 264,00**. Day off aparece com zero. Imposto e custos do carro não são descontados da cobrança.
6. Use **Prévia PDF** para abrir em um leitor de PDF instalado. Depois, **Emitir e guardar PDF** registra o invoice e preserva o documento. O rascunho fica pronto para o próximo número.
7. Em **Invoices emitidos**, toque em **Enviar ao cliente**, escolha o aplicativo de e-mail, confira o PDF anexado, destinatário e valores, e envie pelo aplicativo escolhido. Abrir o compartilhamento não envia automaticamente.
8. Use **Salvar cópia** para guardar o PDF fora do aplicativo. Desinstalar ou limpar dados remove os PDFs privados. Não há sincronização com o programa do computador.

## Atualizar pelo aplicativo

Se ainda não tiver o botão de atualização, instale a versão atual uma vez pelo APK publicado em https://github.com/levymtmr/menatwork/releases/latest . Depois:

1. Abra **Ajustes → Atualizar aplicativo**.
2. Se houver uma versão mais nova, o app baixa o APK oficial e verifica sua assinatura e integridade.
3. Na primeira vez, o Android pode pedir para permitir instalações a partir do Menatwork. Ative a permissão e volte ao aplicativo.
4. Confirme **Atualizar** na tela do instalador do Android. Essa confirmação do sistema é necessária.
5. Se cancelar, use **Instalar atualização** para abrir a etapa novamente. Se estiver sem internet, tente mais tarde.

Atualizar preserva registros e invoices. Não desinstale a versão anterior. O botão consulta apenas as versões publicadas como Releases oficiais, não cada alteração de código no GitHub.


## Dias esquecidos, correções e XP

Em **Histórico → Adicionar dia de trabalho**, informe a data passada e as horas totais ou os horários com pausas. Inclua local, km e gastos. Depois use **Adicionar ao invoice** para cobrar na sexta-feira. **Editar** corrige qualquer jornada salva. **Remover** tira dos números e do XP; a lixeira permite restaurar.

Se esqueceu o cronômetro ligado, em **Hoje → Corrigir horários / encerrar** informe quando realmente terminou. **Descartar jornada atual** remove um turno iniciado por engano.

Na aba **Invoice**, use **Remover** para tirar uma fatura dos totais ou **Corrigir / novo rascunho** para emitir uma substituta com número novo. Confira os lançamentos antes de emitir. O PDF anterior fica preservado; um documento já enviado ao cliente não muda automaticamente. Ao alterar uma jornada vinculada, o invoice é sinalizado para revisão.

Na aba **Skills**, crie suas habilidades. Vincule-as ao turno ou a jornadas do histórico. Cada hora paga vale 100 XP e cada 1.000 XP sobe um nível. Ajustes e exclusões recalculam a experiência; o XP do turno atual é provisório até salvar.


## Temas e pagamentos

Em **Ajustes → Temas**, escolha Pip-Boy, Medieval ou Cyberpunk. A escolha fica salva automaticamente.

O Histórico mostra se a jornada está no rascunho, em um invoice emitido ou paga. Remover a linha do rascunho ou cancelar o invoice remove seu vínculo. No invoice emitido, use **Marcar como pago**; suas jornadas também receberão a marca de pagamento. **Marcar como não pago** desfaz essa ação. O resumo mostra quanto foi pago e quanto falta receber.

O idioma inicial é inglês. Em **Settings → App language**, selecione **Português** se preferir. O PDF do invoice continua sempre em inglês.


## Almoço e filtros (1.6.0)

Ao encerrar, escolha se houve almoço não pago e quantos minutos descontar (45 sugeridos ao escolher Sim). Não precisa pausar para o almoço. Se já havia excluído esse tempo por pausa ou ajuste, não desconte novamente. O campo de almoço também existe no editor de um dia salvo.

Em Hoje → Ajustar jornada e custos, informe cliente e tipo de trabalho. Você também pode completar esses campos no Histórico. Abra Filtros e busca para listar por semana, mês, datas, cliente, tipo e situação. Role até o fim para carregar os próximos 20 registros, ou use Carregar mais. O mesmo vale para invoices.

O novo tema é Retro Console, em Settings → Themes. Escolhas de tema anteriores são mantidas na atualização; basta selecionar Retro Console para usar o novo visual.


## Reutilizar locais (1.6.1)

Depois de salvar um endereço em uma jornada, escolha **Locais salvos / Saved work sites** no próximo turno ou registro. O seletor preenche o campo de endereço; confira e salve normalmente. Os locais anteriores são reconhecidos automaticamente e continuam no celular.


## Cards e clientes (1.6.2)

Use **Expandir / Expand** para abrir detalhes e ações de uma jornada ou invoice e **Recolher / Collapse** para compactar novamente. A seleção de clientes usados aparece nos editores e no topo do rascunho de invoice. Confira os dados antes de emitir.


## Cadastrar clientes (1.7.0)

Abra **Ajustes → Clientes → Novo cliente** (Settings → Clients → New client). Preencha o nome e, se quiser, e-mail, endereço e ABN. Salve e escolha o cliente na lista do turno ou invoice. O botão **＋** ao lado da seleção também abre o cadastro e já seleciona o cliente criado. Em Ajustes, use Editar para atualizar seus dados. Os PDFs emitidos permanecem preservados.

Para clientes com adicional, ative **Overtime** no cadastro. Escolha o cliente antes de iniciar o trajeto. Ao finalizar, informe se houve almoço, os minutos e o horário de início; não é necessário pausar para o almoço. O painel **Esta semana** soma todos os dias trabalhados, incluindo a jornada em andamento.

Para Marken, escolha **Regra de pagamento → Marken / SA Recruitment** no cadastro do cliente. O almoço de 30 minutos é descontado automaticamente apenas em turnos acima de 4 horas; não é necessário informar 45 minutos nem pausar o almoço.

No invoice, escolha o cliente, use Selecionar jornadas ou Criar jornada e revise o bruto. Para Marken, confira as prévias de invoice e timesheet antes de emitir. Depois, salve o timesheet para obter a assinatura e use Anexar timesheet assinado. Enviar ao cliente abre os dois anexos separados no aplicativo escolhido, onde você revisa e envia.
