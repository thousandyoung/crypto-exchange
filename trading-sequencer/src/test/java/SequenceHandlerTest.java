import com.howellyoung.exchange.entity.trade.EventEntity;
import com.howellyoung.exchange.message.event.BaseEvent;
import com.howellyoung.exchange.messaging.MessageTypes;
import com.howellyoung.exchange.repository.trade.EventRepository;
import com.howellyoung.exchange.repository.trade.UniqueEventRepository;
import com.howellyoung.exchange.sequencer.SequenceHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
public class SequenceHandlerTest {
    private UniqueEventRepository uniqueEventRepository;
    private EventRepository eventRepository;
    private SequenceHandler sequenceHandler;

    @Before
    public void setUp() {
        uniqueEventRepository = Mockito.mock(UniqueEventRepository.class);
        eventRepository = Mockito.mock(EventRepository.class);
        sequenceHandler = new SequenceHandler(uniqueEventRepository, eventRepository);
    }
    @Test
    public void testSequenceMessages() throws Exception {
        // Arrange
        List<BaseEvent> messages = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            BaseEvent event = new BaseEvent();
            event.uniqueId = "id" + i;
            messages.add(event);
        }

        when(uniqueEventRepository.findById(anyString())).thenReturn(Optional.empty());
        when(eventRepository.saveAll(any())).thenReturn(null);

        // Act
        List<BaseEvent> result = sequenceHandler.sequenceMessages(new MessageTypes(), new AtomicLong(), messages);

        // Assert
        assertEquals(8, result.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, result.get(i).sequenceId);
        }
        verify(uniqueEventRepository, times(8)).findById(anyString());
        verify(eventRepository, times(1)).saveAll(any());
    }

    @Test
    public void testGetMaxSequenceId() {
        // Arrange
        EventEntity eventEntity1 = new EventEntity();
        eventEntity1.sequenceId = 1L;
        eventEntity1.createdAt = 1L;

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.sequenceId = 2L;
        eventEntity2.createdAt = 2L;

        EventEntity eventEntity3 = new EventEntity();
        eventEntity3.sequenceId = 3L;
        eventEntity3.createdAt = 3L;

        when(eventRepository.findTopByOrderBySequenceIdDesc())
                .thenReturn(Optional.of(eventEntity1), Optional.of(eventEntity2), Optional.of(eventEntity3));

        // Act & Assert
        long result = sequenceHandler.getMaxSequenceId();
        assertEquals(1L, result);

        result = sequenceHandler.getMaxSequenceId();
        assertEquals(2L, result);

        result = sequenceHandler.getMaxSequenceId();
        assertEquals(3L, result);

        verify(eventRepository, times(3)).findTopByOrderBySequenceIdDesc();
    }
}