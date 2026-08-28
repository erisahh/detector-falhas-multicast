Detector de falhas por omissão usando multicast IP


Descrição:

Implementação de um detector de falhas por omissão utilizando comunicação multicast IP. O monitor possui duas threads responsáveis pelo envio e recebimento periódico de mensagens:
SenderThread: envia mensagens para um grupo multicast.
ReceiverThread: recebe mensagens do grupo multicast, identifica o endereço IP do remetente e conta a quantidade de mensagens recebidas.
Foi utilizado o grupo multicast 224.0.0.2 e a porta 8881.


Funcionamento
A SenderThread cria e envia periodicamente uma mensagem UDP para o endereço multicast: 224.0.0.2:8881

No código é demonstrado que uma nova mensagem é enviada a cada 50 milissegundos. Além disso, cada mensagem possui um contador incremental para identificar a sequência dos envios.
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
