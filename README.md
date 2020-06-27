# kotlin_undo

Copyright (c) 2020 by Fred George  
MIT License - see LICENSE file  
@author Fred George  fredgeorge@acm.org 

## Overview

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

## Interfaces

### Undoable

This is the basic Command interface, with a basic API:

    execute()   // Run the command; returns true if successful, false if failed, and null if suspended
    undo()      // Reverse the effects of this command; returns true if successful, and false otherwise
    resume(r: R)    // Resume a suspension; same return meanings as execute(); defaults to execute()

In addition, there are several more APIs:

    accept(v: CommandVisitor)   // Supporting the Visitor pattern
    inject(b: Behavior)         // Supporting the Decorators for Behaviors; useful for tracing
    
Finally, there is also an identifier for ease of debugging. It can be anything.

### Undoable.Behavior

The optional Behavior interface is used by some of the stock Command classes to allow Command
implementations without having to subclass to get stock state- or composite-based classes. It
has a similar basic API to Undoable:

    executeAction()     // execute() has been invoked; same return values
    undoAction()        // undo() has been invoked; same return values
    resumeAction(r: R)  // resume(r) has been invoked; same return values
    cleanupAction()     // invoked upon completion of the above actions; not invoked if suspended
    accept(v: CommandVisitor)   // Supporting the Visitor pattern
    
### Undoable.Composite

This interface is intended for classes representing groups of Command steps. In addition to the
base *Undoable* methods, it also has the simple list manipulators modeling the same MutableList
behaviors including return values:

    add(step: Undoable)
    add(index: Int, step: Undoable)
    remove(step: Undoable)

### Undoable.Trace

This is simply a marker interface for tracing via the Decorator pattern. It is simply another
Behavior that will be injected between the functional behavior and the stock Command classes.
