.class public Factorial
.super java/lang/Object


.method public static ComputeFac(I)I
  .limit stack 999
  .limit locals 3




  iconst_1
  if_icmpge loop0_end



  goto loop0_next

loop0_end:



  goto loop1_next

loop1_end:
  aload_2
  imul


loop1_next:

loop0_next:

loop2:
  iconst_1
  if_icmpge loop2_end



  goto loop2

loop2_end:

  iload_3
  ireturn
.end method


.method public static main([Ljava/lang/String;)V
  .limit stack 999


AHAHAHAHAHAHAHAHAHAHAHA <>


  bipush 10
  invokevirtual Factorial/ComputeFac(I)I
  invokestatic io/println()V


  return
.end method

