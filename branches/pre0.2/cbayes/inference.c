/*
 * inference.c
 * 
 * Copyright (C) 2006 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */


/*
 * Calculate conditional probability distribution given probability function
 * and conditioning distributions.
 *
 * RUNNING TIME: mul = O(dist->len*SIGMA(dist[i]->len))
 *
 * @param func
 *				probability function
 * @param dist
 *				array of probability distributions
 * @param cond
 *				target probability distribution
 * @return
 *				0 on success, or error code
 */
int cbayes_conditional(pfunc *func, pdist **dist, pdist *cond)
{
	// cache pointers, precompute
	int i, j, b;
	int clen = func->condlen;
	int nvars = func->nvars;
	int nvn1 = nvars - 1;
	int *idx = func->idx;
	int *len = func->len;
	double *val = func->val;
	double *fdist = func->dist;
	double **cdist = func->cdist;
	double *tdist = cond->dist;
	for (i=0; i<func->nvars; i++)
		cdist[i] = dist[i]->dist;

	// initialize to zeros
	ifill(idx, nvars, 0);
	ffill(tdist, clen, 0.0);
	
	// loop every conditional combination
	for (i=0; i<clen; i++)
	{
		// increment recursive index
		j = nvn1;
		while (j >= 0 && (++idx[j] == len[j+1]))
			idx[j--] = 0;
		// multiple conditional probabilities together
		for (; j<nvars; j++)
			val[j+1] = val[j] * cdist[j][idx[j]];
		// multiply by function into target
		for (j=0, b=i; j<len[0]; j++, b+=cond->len)
			tdist[j] += cdist[b] * val[nvars];
	}

	// correct within normal tolerance
	correct(cond->dist, cond->len); 

	return 0;
}

int cbayes_markov_init(bvar *var)
{
	int i, j;
	bvar *c;

	if (var->observed)
		return 0;
	
	ifill(var->state_count, var->dist->len, 0);
	
	if (var->mb_child_base != NULL) free(var->mb_child_base);
	if (var->mb_child_idx != NULL) free(var->mb_child_idx);
		
	var->mb_child_base = (int*) malloc(sizeof(int) * var->nchildren);
	var->mb_child_idx = (int*) malloc(sizeof(int) * var->nchildren);

	for (i=0; i<var->nchildren; i++)
	{
		c = var->children[i];
		var->mb_child_base[i] = 1;

		for (j=c->nparents-1; j>=0; j--)
			if (c->parents[j] == var) break;
			else var->mb_child_base[i] *= c->parents[j]->dist->len;
	}
}

int cbayes_fdist_index(bvar *var)
{
	int i, idx = 0, base = 1;
	bvar *p;

	for (i=var->nparents-1; i>=0; i--)
	{
		p = var->parents[i];
		idx += base * p->state;
		base *= p->dist->len;
	}

	return idx;
}

int cbayes_markov_blanket(bvar *var)
{
	int i, j, pt = cbayes_fdist_index(var);
	int base, jump = var->func->condlen;
	double *cond = var->func->dist;
	double *dist = var->dist->dist;
	double **cpri = var->cpri;
	double p, q, sum;
	bvar *c;

	// initialize child indices
	for (i=0; i<var->nchildren; i++)
	{
		c = var->children[i];
		var->mb_child_idx[i] =	cbayes_fdist_index(c) +
								c->func->condlen * c->state;
	}

	base = 0;
	sum = 0.0;

	// iterate possible states
	for (i=0; i<dist->len; i++)
	{
		// conditional probability of node given parents
		p = cond[base + pt];
		base += jump;

		// conditional probability of children given node
		for (j=0; j<var->nchildren; j++)
		{
			p *= cpri[j][var->mb_child_idx[j]];
			var->mb_child_idx[j] += var->mb_child_base[j];
		}

		// save to posterior distribution
		sum += dist[i] = p;
	}

	// normalize
	if (sum > 0.0)
		for (i=0; i<var->dist->len; i++)
			dist[i] /= sum;

	// choose next state
	q = cbayes_rand();
	p = 0.0;
	for (i=0; i<var->dist->len; i++)
	{
		p += dist[i];
		if (p > q)
			break;
	}

	// count state for average
	var->state = i;
	var->state_count[var->state]++;

	return 0;
}
