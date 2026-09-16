# Validação da versão 1.0.0

## Ambiente

- Samsung Galaxy A35 (SM-A356E), Android 16: instalação, permissões, disparo por AlarmManager, abertura em tela cheia com tela bloqueada e início de áudio nativo.
- Emulador Android 14: formulários, calendário, persistência, recorrência, reinicialização e importação pelo seletor Android.
- JDK 17 / SDK 35 / Gradle 8.14.3 / Android Gradle Plugin 8.7.2.

## Evidências

- `python scripts/test.py`: **916 asserções aprovadas**. Dois empregos 12×36, múltiplos toques, datas excluídas, virada de dia, 24×48, ciclos de 36 horas, semana com horários por dia, ano bissexto, horário de verão e revezamento diurno/noturno. São testes do motor de cálculo, não 916 despertares físicos.
- `assembleDebug assembleRelease lintDebug`: compilação aprovada, **zero erros de lint**. Avisos remanescentes incluem APIs depreciadas, uso deliberado de WebView/JavaScript e otimização de bateria.
- A35: disparo agendado às 23:47:06 UTC, recebido às 23:47:06.365 UTC; `AUDIO_ATIVO` às 23:47:07.174 UTC. `RingActivity` tornou-se a atividade em primeiro plano sobre o bloqueio. O teste verifica o estado de reprodução do Android; não mede o volume acústico do alto-falante.
- Emulador: plantão de teste às 23:49:15 UTC disparou às 23:49:15.013; a próxima ocorrência foi registrada para dois dias depois **antes de iniciar o áudio**.
- Após reinicialização real do emulador, tanto `LOCKED_BOOT_COMPLETED` quanto `BOOT_COMPLETED` restauraram as três escalas de teste.
- Áudio WAV, imagem PNG e vídeo MP4 foram selecionados em Downloads pelo seletor de arquivos do Android e copiados para o app.
- Foto escolhida no seletor exibida como fundo da tela de despertar, conferida em captura.
- Vídeo de fundo e áudio local: `DISPARO`, `AUDIO_ATIVO` e `VIDEO_ATIVO` observados no mesmo alarme; botões de soneca e desligamento conferidos visualmente.
- Soneca de um minuto gerou um agendamento separado e voltou a disparar às 23:59:10.397 UTC, para um horário esperado de 23:59:10.391 UTC. O trabalho continuou com `enabled=true` e sua próxima recorrência.
- YouTube: seleção do vídeo retornou seu ID ao formulário e o player oficial reproduziu vídeo na tela do alarme do emulador; interface exibiu legendas durante a reprodução. Isso não garante autoplay para todos os vídeos/aparelhos.
- Dois trabalhos com cores diferentes cadastrados pela interface; calendário exibiu sobreposições de plantões noturnos.

## Limites da verificação

- O teste de Doze forçado do emulador retornou `INACTIVE`; não é apresentado como validação completa de Doze profundo.
- Não houve teste de semanas seguidas nem de todos os fabricantes/versões do Android.
- YouTube é um serviço externo. Busca/seleção e player estão implementados, mas autoplay, restrições de vídeos, anúncios, conectividade e volume de mídia não têm garantia. O som local é a opção confiável com tela bloqueada; a tela informa isso.
- Reinicialização foi testada no emulador para não reiniciar o aparelho pessoal durante o uso.
- Escalas de demonstração e arquivos de teste pertencem ao emulador; horários de trabalho reais não são presumidos.

Veja [USO.md](USO.md) para fazer o teste de 15 segundos no seu próprio aparelho e para a configuração de bateria do Samsung.
