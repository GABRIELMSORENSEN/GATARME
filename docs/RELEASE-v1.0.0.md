# GATARME 1.0.0

Despertador para múltiplos empregos e escalas recorrentes, com calendário por cor, horários semanais, ciclos livres e revezamento de turnos.

## Incluído

- Plantões 12×36 e outros modelos, sem precisar remarcar após cada toque.
- AlarmManager nativo, restauração no boot, tela cheia, soneca e áudio offline de reserva.
- Áudio local, foto/vídeo na tela do alarme e tema personalizável.
- Seleção de YouTube dentro do app; reprodução online em tela desbloqueada, com áudio local de segurança.
- Guia de permissões e proteção de bateria.

## Instalação

Baixe **GATARME-v1.0.0.apk**. Requer Android 8.1+. O APK é assinado para instalação e futuras atualizações. O arquivo **SHA256SUMS.txt** permite verificar a integridade.

Depois de instalar, abra **Proteção**, conceda as permissões e execute o teste de 15 segundos. Cadastre as datas e os horários reais de cada emprego. No Samsung, adicione o app em **Aplicativos nunca suspensos**.

Compilação e lint aprovados; 916 asserções do motor de escalas. Disparo e tela cheia testados em Samsung A35/Android 16; restauração após reboot e mídia local testadas em Android 14 virtual. [Relatório de validação](https://github.com/GABRIELMSORENSEN/GATARME/blob/main/docs/VALIDACAO.md).

YouTube não é baixado nem convertido em áudio offline. Celular desligado, volume zerado, “Forçar parada”, políticas de bateria ou bloqueios do YouTube podem impedir funcionalidades; use áudio local e teste no seu aparelho.
