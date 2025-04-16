import 'dart:async';

/// Enum representing the different types of Crisp events
enum CrispEventType {
  chatboxOpened,
  chatboxClosed,
  messageSent,
  messageReceived,
  sessionEvent,
  sessionReset,
  unreadCountChanged,
}

/// Class representing a Crisp event
class CrispEvent {
  /// The type of the event
  final CrispEventType type;

  /// Additional data associated with the event, if any
  final Map<String, dynamic>? data;

  CrispEvent(this.type, this.data);

  @override
  String toString() => 'CrispEvent(type: $type, data: $data)';
}

/// A singleton class that provides a stream of Crisp events
class CrispEvents {
  static final CrispEvents _instance = CrispEvents._();

  /// Stream controller for Crisp events
  final StreamController<CrispEvent> _eventController =
      StreamController<CrispEvent>.broadcast();

  /// Returns the singleton instance of CrispEvents
  factory CrispEvents() => _instance;

  CrispEvents._();

  /// A broadcast stream of Crisp events
  Stream<CrispEvent> get events => _eventController.stream;

  /// Method called by the platform implementation to send events
  void handlePlatformEvent(Map<dynamic, dynamic> event) {
    try {
      final String eventName = event['event'] as String;
      final dynamic eventData = event['data'];

      final CrispEventType type = _parseEventType(eventName);
      final Map<String, dynamic>? data = eventData != null
          ? Map<String, dynamic>.from(eventData as Map)
          : null;

      _eventController.add(CrispEvent(type, data));
    } catch (e) {
      print('Error parsing Crisp event: $e');
    }
  }

  /// Parse event name string to CrispEventType enum
  CrispEventType _parseEventType(String eventName) {
    switch (eventName) {
      case 'onChatboxOpened':
        return CrispEventType.chatboxOpened;
      case 'onChatboxClosed':
        return CrispEventType.chatboxClosed;
      case 'onMessageSent':
        return CrispEventType.messageSent;
      case 'onMessageReceived':
        return CrispEventType.messageReceived;
      case 'onSessionEvent':
        return CrispEventType.sessionEvent;
      case 'onSessionReset':
        return CrispEventType.sessionReset;
      case 'onUnreadCountChanged':
        return CrispEventType.unreadCountChanged;
      default:
        throw Exception('Unknown event type: $eventName');
    }
  }

  /// Dispose of resources
  void dispose() {
    _eventController.close();
  }
}
