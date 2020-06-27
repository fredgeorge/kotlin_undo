# kotlin_undo

Copyright (c) 2020 by Fred George  
MIT License - see LICENSE file  
@author Fred George  fredgeorge@acm.org 

This is a sample Kotlin implementation of Macro pattern (Command + Composite) with undo capability.
A Visitor is provided to walk the Command hierarchy, along with an exploitation of this
Visitor pattern for pretty-printing the Command hierarchy. Also featured are use of a
Null Object pattern and Decorator pattern.

Key design objectives:

- **Separation of behavior from the Command hierarchy classes:** The intent is to **not** 
burn the single inheritance *chit* of Kotlin on subclassing Command classes. Rather, 
Command classes *buy* their behavior from separate Behavior classes. This allows Behaviors
to use inheritance if convenient.
- **Consolidation of Behaviors:** Much like Visitor implementations, the complete
implementation of a Behavior is consolidated in one place.
- **Tracing Behaviors:** Composite structures with their natural recursion can be difficult
to debug. This framework uses a Decorator pattern to provide the aspect-like ability to trace
behaviors, along with a sample ActionTracer.
