# Decision Log

## Decision 000 - Team Formation
Date: Prior to 22 August 2026
Status: Closed

**Context:** While forming the team, some students available were strong coders. 
The team leader (Cyrus) deliberately did not select them, on the assessment that mixing strong and less-experienced coders on one team risked ego conflicts that could hurt team cohesion. The current team (Cyrus, Arnold, Shashidhara, Chrisel) was formed instead, with the expectation that responsibilities would need to be actively guided rather than assumed.

**Decision:** Proceed with the current team composition, and compensate for the mixed experience level with a structured task-division method (see Decision 002) rather than relying on self-organization.

**Reason:** Prioritizing team cohesion and manageable collaboration over raw coding strength, given the mismatch would have to be managed for the full project duration.

**Effect on project:** The team leader takes a more active guiding/coordinating role than a typical hands-off team lead - breaking work into small, scored tasks (see Decision 002) rather than assigning large open-ended modules. 

---

## Decision 001 - Programming Language
**Date:** 22 August 2026
**Status:** Closed

**Options considered:** Java (preferred/assigned) vs. an alternative language.

**Decision:** Java selected.

**Reason:** Java is the assigned preferred language; team has classroom familiarity with it; strong OOP model fits simulating CPU/registers/memory/processes as classes and objects.

**Effect on project:** All core simulator modules (CPU, memory, process manager, scheduler) will be implemented in Java. Any future proposal to use a different language for a specific module requires a documented reason, advantages/limitations analysis, and faculty approval before implementation - no language change will be made informally.

---

## Decision 002 - Research Topic Allocation (Week 1)
**Date:** 22 August 2026
**Status:** Open (pending confirmation from Arnold)

**Problem:** The 10 required STC89C52 study topics needed to be split among 4 members.

**Discussion:** Since the team leader wasn't sure how to divide unfamiliar technical work fairly, each of the 10 topics was first scored by difficulty - **Hard** (memory organization, timer, interrupt mechanism), **Medium**, **Easy** - then members were given the freedom to choose from the scored list, with the team leader balancing the final split so no one was overload. Arnold was not present at the meeting, so his topics are provisional. 

**Decision:**
- Shashidhara → Memory organization, Program Counter
- Chrisel → Timer, Stack mechanism / Stack pointer
- Cyrus → Interrupt mechanism (+ balancing support)
- Arnold → CPU/register organization, Status/flag information,
Instruction categories, GPIO *(pending his confirmation)*

**Responsible:** Cyrus (to confirm with Arnold)

**Next step:** Update this entry to "Closed" once Arnold confirms or the allocation is renegotiated.

---

## Decision 003 - Build Approach: Reference C Behavior → Java Web Simulator
**Date:** 27 August 2026
**Status:** Open

**Problem:** The team needs a concrete method for going from "we don't understand the STC89C52" to "we have a working Java-based simulator," without just guessing at behavior or, on the other end, producing something that looks AI-generated with no grounding in the real chip.

**Approach:** Study real STC89C52 embedded C code to understand exact register/peripheral behavior. Then reimplement the equivalent *behavior* - not the C source itself -  as Java classes exposed through a web front end, rather than running on real hardware.

**Reason:** Keeps the simulator's behavior grounded in the real STC89C52 datasheet/reference code while still delivering the actual required artifact: a Java, web-visualized simulator - not a hardware deployment.

**Effect on project:** Each research topic write-up (Decision 002 assignments) should include, where relevant, a short C snippet showing the real register/peripheral behavior being modeled, alongside the planned Java class/method that will simulate it. This becomes the bridge between "architecture study" (Week 1) and "implementation" (Week 2+).

**Responsible:** Whole team - each member documents this mapping for their assigned topic(s).

---

## Decision 004 - Finalized Build Approach (27 Aug meeting, 4:20 PM)
**Date:** 27 August 2026
**Status:** Closed

**Discussion:** The full team met and discussed the direction proposed in Decision 003, walking through it together until everyone understood it. 

**Decision:** Finalized approach - obtain real STC89C52 source code, run it through a compiler to observe and understand its actual behavior, then build a Java-based UI that models that behavior and connects to it as the simulator front end.

**Reason:** Compiling and observing real source code first (rather than only reading the datasheet) gives the team a concrete, verifiable reference for how each instruction/peripheral actually behaves, before committing that behavior to Java classes.

**Effect on project:**  Decision 003 with a concrete step: compile and run reference source code as part of each member's research, not just read about it.

**Responsible:** Whole team.
