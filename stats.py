#
# Stats for ICPC Challenge
#

count = 0
scores = [0, 0]
wins = 0

with open('output.txt') as f:
	for line in f.readlines():
		if (line.startswith('Winner:')):
			continue
		tokens = line.split(' ')
		us = int(tokens[1])
		them = int(tokens[4])
		if us > them:
			wins = wins + 1
		scores[0] = scores[0] + us
		scores[1] = scores[1] + them
		count = count + 1

print("%d matches" % count)
print("	%d wins, average score %5.1f" % (wins, scores[0] / count))
print("	%d wins, average score %5.1f" % (count - wins, scores[1] / count))
