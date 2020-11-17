import sys
import math
import random

def log(msg):
    print(msg, file=sys.stderr, flush=True)

def canMake(order, inv):
    n = 0
    for x in zip(order, inv):
        if x[0] + x[1] < 0: return False
        n += x[0] + x[1]
    return n <= 10

# game loop
while True:
    orders = []
    spells = []
    action_count = int(input())  # the number of spells and recipes in play
    for i in range(action_count):
        action_id, action_type, delta_0, delta_1, delta_2, delta_3, price, tome_index, tax_count, castable, repeatable = input().split()
        action_id = int(action_id)
        delta_0 = int(delta_0)
        delta_1 = int(delta_1)
        delta_2 = int(delta_2)
        delta_3 = int(delta_3)
        price = int(price)
        tome_index = int(tome_index)
        tax_count = int(tax_count)
        castable = castable != "0"
        repeatable = repeatable != "0"
        item = {
            'id': action_id,
            'delta': [delta_0, delta_1, delta_2, delta_3],
            'price': price,
            'castable': castable
        }
        if action_type == 'BREW':
            orders.append(item)
        elif action_type == 'CAST':
            spells.append(item)

    inv_0, inv_1, inv_2, inv_3, my_score = [int(j) for j in input().split()]
    inv = [inv_0, inv_1, inv_2, inv_3]
    opp_0, opp_1, opp_2, opp_3, opp_score = [int(j) for j in input().split()]

    log(f"inv: {inv}")
    choices = [o for o in orders if canMake(o['delta'], inv)]
    choices.sort(key = lambda x: -x['price'])

    log(f"choices: {choices}")

    if len(choices) > 0:
        print("BREW " + str(choices[0]['id']))
        continue

    choices = [o for o in spells if canMake(o['delta'], inv) and o['castable'] > 0]
    log(f"choices: {choices}")
    if len(choices) > 0:
        choice = random.choice(choices)
        log(choice)
        print("CAST " + str(choice['id']))
        continue

    non_castables = [o for o in spells if o['castable'] == 0]
    if len(non_castables) > 0:
        print("REST")
        continue

    print("WAIT")
