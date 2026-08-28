<<<<<<< HEAD
Detector de falhas por omissão usando multicast IP


Descrição:

Implementação de um detector de falhas por omissão utilizando comunicação multicast IP. O monitor possui duas threads responsáveis pelo envio e recebimento periódico de mensagens:
SenderThread: envia mensagens para um grupo multicast.
ReceiverThread: recebe mensagens do grupo multicast, identifica o endereço IP do remetente e conta a quantidade de mensagens recebidas.
O grupo multicast padrão utilizado é 224.0.0.2 e a porta é 8881.


Funcionamento
A SenderThread cria e envia periodicamente uma mensagem para o endereço configurado.

No código é demonstrado que uma nova mensagem é enviada a cada 50 milissegundos. Além disso, cada mensagem possui um contador incremental para identificar a o número dos envios.
A ReceiverThread permanece associada ao grupo multicast e aguarda o recebimento das mensagens. Quando uma mensagem é recebida, o endereço IP do remetente é identificado e o contador daquele IP é incrementado.


Cálculo da disponibilidade

O monitor mantém, para cada endereço IP conhecido, as seguintes informações:
  contador: quantidade de mensagens recebidas durante o intervalo atual;
  máximo: maior quantidade de mensagens recebidas em um único intervalo anteriormente observado;
  disponibilidade: razão entre o número atual de mensagens recebidas e o maior número de mensagens já observado. A disponibilidade é calculada por: disponibilidade = contador / máximo. Assim, quanto mais próximo de 1 (100%) estiver o valor, maior foi a quantidade de mensagens recebidas em relação ao melhor intervalo observado.


Estruturas utilizadas

As informações são armazenadas em mapas, utilizando o endereço IP como chave:
  // IP -> disponibilidade
  private static Map<String, Double> disponibilidade = new HashMap<>();
  // IP -> contador de eventos durante o intervalo
  private static Map<String, Double> contador = new HashMap<>();
  // IP -> máximo de eventos observados
  private static Map<String, Double> max = new HashMap<>();


Concorrência

Como SenderThread e ReceiverThread acessam simultaneamente os mapas compartilhados, foi utilizado um objeto de sincronização:
private static final Object lock = new Object();
O acesso de leitura e escrita ao contador e às demais estruturas relacionadas é realizado dentro de blocos synchronized, evitando condições de corrida entre as duas threads.
=======
# Detector de Falhas com Multicast

Cada processo envia mensagens para um grupo
multicast e mede a disponibilidade dos outros processos do grupo.

## Como executar

```sh
javac Monitor.java
java Monitor [multicast-ip] [intervalo-ms]
```

Parâmetros de entrada:

| Comando | Comportamento |
| --- | --- |
| `java Monitor` | IP padrão (`224.0.0.2`) e intervalo aleatório |
| `java Monitor <multicast-ip>` | IP informado e intervalo fixo de 1000ms |
| `java Monitor <multicast-ip> <intervalo-ms>` | IP e intervalo fixo informados |

## Parâmetros

- `multicast-ip`: endereço do grupo multicast. Todos os processos precisam usar
  o mesmo. Padrão `224.0.0.2`.
- `intervalo-ms`: pausa entre os envios de mensagens e também a janela usada
  pelo monitor para calcular a disponibilidade. Padrão `1000`.
- Sem parâmetros o intervalo de envio vira aleatório entre 1ms e
  1000ms. Serve para simular um processo instável e ver a disponibilidade cair.
  A janela do monitor continua em 1000ms.

A porta (`8881`) e a interface de rede são constantes no início do
[Monitor.java](Monitor.java).

## Como funciona

São três threads:

- **SenderThread**: envia uma mensagem para o grupo multicast a cada intervalo.
- **ReceiverThread**: entra no grupo, escuta as mensagens e incrementa um contador por IP de origem.
- **MonitorThread**: a cada intervalo lê os contadores, calcula a disponibilidade, imprime e zera os contadores.

## Cálculo da disponibilidade

O maior número de mensagens já recebido de um IP em um intervalo de tempo representa o 100% de disponibilidade.
A disponibilidade é `mensagens_no_intervalo / maximo_ja_visto`.
>>>>>>> 185b85b (Added README)
